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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import ch.alni.certblues.acme.client.AcmeServerException;
import ch.alni.certblues.acme.client.Directory;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Interface to get nonce from the ACME server.
 */
class Session {
    private static final Logger LOG = getLogger(Session.class);

    private final HttpClient httpClient;
    private final Duration requestTimout;
    private final Directory directory;

    private final AtomicReference<String> nonceRef = new AtomicReference<>("");

    Session(HttpClient httpClient, Duration requestTimout, Directory directory) {
        this.httpClient = httpClient;
        this.requestTimout = requestTimout;
        this.directory = directory;
    }

    Directory getDirectory() {
        return directory;
    }

    String getNonce() {
        return nonceRef.get();
    }

    void updateNonce() {
        LOG.info("getting a new nonce from the ACME server");

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(directory.newNonce()))
                .timeout(requestTimout)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        final HttpResponse<String> response = Requests.withErrorHandling(
                () -> httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        );

        final int statusCode = response.statusCode();
        if (statusCode == 200) {
            doUpdateNonce(response);
            LOG.info("a new nonce returned: {}", nonceRef.get());
        }
        else if (statusCode < 400) {
            LOG.warn("unexpected status code {} returned", statusCode);
            doUpdateNonce(response);
        }
        else {
            Payloads.extractError(response).ifPresent(
                    error -> {
                        throw new AcmeServerException(error);
                    });

            throw new AcmeServerException(response.body());
        }
    }

    /**
     * Executes request and updates the session with the latest nonce.
     */
    HttpResponse<String> executeRequest(Callable<HttpResponse<String>> requestCall) {
        final HttpResponse<String> response = Requests.withErrorHandling(requestCall);
        doUpdateNonce(response);
        return response;
    }

    private void doUpdateNonce(HttpResponse<?> response) {
        Payloads.findNonce(response).ifPresent(nonceRef::set);
    }
}
