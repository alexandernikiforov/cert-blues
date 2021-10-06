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

package ch.alni.certblues.acme.client.impl;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

import ch.alni.certblues.acme.client.Error;
import ch.alni.certblues.acme.json.JsonObjects;
import reactor.netty.http.client.HttpClientResponse;

/**
 * Static utility class to work with ACME request and response payloads.
 */
final class Payloads {

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_LOCATION = "Location";
    private static final String HEADER_REPLAY_NONCE = "Replay-Nonce";

    private Payloads() {
    }

    static Optional<Error> extractError(HttpResponse<String> response) {
        final boolean responseIsJson = response.headers().firstValue(HEADER_CONTENT_TYPE)
                .filter(header -> header.startsWith("application/problem+json"))
                .isPresent();

        if (responseIsJson) {
            final var error = JsonObjects.deserialize(response.body(), Error.class);
            return Optional.of(error);
        }
        else {
            return Optional.empty();
        }
    }

    static Optional<Error> extractError(HttpClientResponse response, String body) {
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

    static Optional<String> findLocation(HttpResponse<?> response) {
        return response.headers().firstValue(HEADER_LOCATION);
    }

    static Optional<String> findNonce(HttpResponse<?> response) {
        return response.headers().firstValue(HEADER_REPLAY_NONCE);
    }

}
