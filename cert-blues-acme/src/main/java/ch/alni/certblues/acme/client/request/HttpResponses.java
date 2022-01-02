/*
 * MIT License
 *
 * Copyright (c) 2020, 2021 Alexander Nikiforov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package ch.alni.certblues.acme.client.request;

import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;

import ch.alni.certblues.acme.protocol.AcmeClientException;
import ch.alni.certblues.acme.protocol.AcmeServerException;
import ch.alni.certblues.acme.protocol.Error;
import ch.alni.certblues.common.json.JsonObjectException;
import ch.alni.certblues.common.json.JsonObjects;
import reactor.netty.http.client.HttpClientResponse;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Static utility class to work with ACME HTTP responses.
 */
final class HttpResponses {
    private static final Logger LOG = getLogger(HttpResponses.class);

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_LOCATION = "Location";
    private static final String HEADER_REPLAY_NONCE = "Replay-Nonce";

    private HttpResponses() {
    }

    /**
     * Returns the JSON payload from the provided body of the given response. The payload will be serialized to the
     * provided class.
     *
     * @param response response object from HTTP client (includes the headers and the status code)
     * @param body     the body of the response as string object
     * @param clazz    the class to serialize the body to
     * @param <T>      the class parameter of the method
     * @return the deserialized object or throws an exception
     * @throws AcmeServerException if the server returned the status code that is greater or equal to 400
     * @throws AcmeClientException if response payload cannot be deserialized
     */
    static <T> T getPayload(HttpClientResponse response, String body, Class<T> clazz) {
        final var statusCode = response.status().code();

        try {
            if (statusCode < 400) {
                LOG.info("status code {}, trying to convert body", statusCode);
                return JsonObjects.deserialize(body, clazz);
            }
            else {
                extractError(response, body).ifPresent(error -> {
                    throw new AcmeServerException(error);
                });

                throw new AcmeServerException(body);
            }
        }
        catch (JsonObjectException e) {
            throw new AcmeClientException("cannot deserialize the ACME response payload", e);
        }
    }

    public static String getPayload(HttpClientResponse response, String body) {
        final var statusCode = response.status().code();

        try {
            if (statusCode < 400) {
                LOG.info("status code {}, trying to convert body", statusCode);
                return body;
            }
            else {
                extractError(response, body).ifPresent(error -> {
                    throw new AcmeServerException(error);
                });

                throw new AcmeServerException(body);
            }
        }
        catch (JsonObjectException e) {
            throw new AcmeClientException("cannot deserialize the ACME response payload", e);
        }
    }

    /**
     * Returns the nonce from the header ("Replay-nonce") of the provided response.
     *
     * @param response response containing the nonce header
     * @return the nonce or null if nothing found
     * @throws AcmeServerException if the server returned the status code that is greater or equal to 400
     */
    static String getNonce(HttpClientResponse response) {
        final var statusCode = response.status().code();

        final String nonce = getHeader(response, HEADER_REPLAY_NONCE);
        LOG.info("a new nonce returned: {}, status code {}", nonce, statusCode);
        return nonce;
    }

    /**
     * Returns the Location header from the response or null if this header is not present.
     */
    static String getLocation(HttpClientResponse response) {
        return getHeader(response, HEADER_LOCATION);
    }

    private static String getHeader(HttpClientResponse response, String header) {
        return response.responseHeaders().getAsString(header);
    }

    private static Optional<Error> extractError(HttpClientResponse response, String body) {
        final boolean responseIsJson = response.responseHeaders().entries().stream()
                .filter(entry -> entry.getKey().equals(HEADER_CONTENT_TYPE))
                .map(Map.Entry::getValue)
                .anyMatch(header -> header.startsWith("application/problem+json"));

        if (responseIsJson) {
            final var error = JsonObjects.deserialize(body, Error.class);
            return Optional.of(error);
        }
        else {
            return Optional.empty();
        }
    }

}
