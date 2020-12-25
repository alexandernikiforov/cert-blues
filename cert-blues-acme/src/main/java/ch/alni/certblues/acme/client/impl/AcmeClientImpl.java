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

import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import ch.alni.certblues.acme.client.AcmeClient;
import ch.alni.certblues.acme.client.AcmeServerException;
import ch.alni.certblues.acme.client.Directory;
import ch.alni.certblues.acme.client.DirectoryHandle;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * ACME client implementation using the standard HTTP client (as of Java 11).
 */
class AcmeClientImpl implements AcmeClient {
    private static final Logger LOG = getLogger(AcmeClientImpl.class);

    private final HttpClient httpClient;
    private final Duration timeout;

    AcmeClientImpl(HttpClient httpClient, Duration timeout) {
        this.httpClient = httpClient;
        this.timeout = timeout;
    }

    @Override
    public DirectoryHandle getDirectory(String url) {
        LOG.info("reading ACME directory at URL {}", url);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .GET()
                .build();

        final HttpResponse<String> response = Requests.withErrorHandling(
                () -> httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        );

        final int statusCode = response.statusCode();

        if (statusCode == 200) {
            LOG.info("directory object successfully returned: {}", response.body());
            return toDirectoryHandle(response);
        }
        else if (statusCode < 400) {
            LOG.warn("unexpected status code {}, trying to convert body", statusCode);
            return toDirectoryHandle(response);
        }
        else {
            Payloads.extractError(response).ifPresent(
                    error -> {
                        throw new AcmeServerException(error);
                    });

            throw new AcmeServerException(response.body());
        }
    }

    private DirectoryHandle toDirectoryHandle(HttpResponse<String> response) {
        final Directory directory = Payloads.deserialize(response.body(), Directory.class);
        final Session session = new Session(httpClient, timeout, directory);

        return RetryableHandle.create(
                session,
                new DirectoryHandleImpl(httpClient, timeout, session, directory),
                DirectoryHandle.class
        );
    }

}
