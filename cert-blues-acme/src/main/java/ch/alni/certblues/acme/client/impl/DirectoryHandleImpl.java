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

import ch.alni.certblues.acme.client.Account;
import ch.alni.certblues.acme.client.AccountHandle;
import ch.alni.certblues.acme.client.AccountRequest;
import ch.alni.certblues.acme.client.AcmeClientException;
import ch.alni.certblues.acme.client.AcmeServerException;
import ch.alni.certblues.acme.client.Directory;
import ch.alni.certblues.acme.client.DirectoryHandle;
import ch.alni.certblues.acme.client.SigningKeyPair;
import ch.alni.certblues.acme.jws.JwsObject;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the ACME directory interface.
 */
class DirectoryHandleImpl implements DirectoryHandle {
    private static final Logger LOG = getLogger(DirectoryHandleImpl.class);

    private final HttpClient httpClient;
    private final Duration requestTimout;
    private final Session session;
    private final Directory directory;

    DirectoryHandleImpl(HttpClient httpClient, Duration requestTimout, Session session, Directory directory) {
        this.httpClient = httpClient;
        this.requestTimout = requestTimout;
        this.session = session;
        this.directory = directory;
    }

    @Override
    public AccountHandle getAccount(SigningKeyPair keyPair, AccountRequest accountRequest) {
        final String newAccountUri = directory.newAccount();

        // on first call retrieve a nonce
        session.updateNonce();

        final String nonce = session.getNonce();

        LOG.info("getting account for URI {} with request {}", newAccountUri, accountRequest);

        // sign request
        final JwsObject jwsObject = keyPair.sign(newAccountUri, accountRequest, nonce);

        final var body = Payloads.serialize(jwsObject);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(newAccountUri))
                .timeout(requestTimout)
                .header("Content-Type", "application/jose+json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final HttpResponse<String> response = session.executeRequest(
                () -> httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        );

        final int statusCode = response.statusCode();
        if (statusCode == 200) {
            LOG.info("an existing account has been successfully returned: {} ", response.body());
            return toAccountHandle(keyPair, response, session);
        }
        else if (statusCode == 201) {
            LOG.info("a new account has been successfully created: {}", response.body());
            return toAccountHandle(keyPair, response, session);
        }
        else if (statusCode < 400) {
            LOG.warn("unexpected status code {} returned", statusCode);
            return toAccountHandle(keyPair, response, session);
        }
        else {
            Payloads.extractError(response).ifPresent(
                    error -> {
                        throw new AcmeServerException(error);
                    });

            throw new AcmeServerException(response.body());
        }
    }

    @Override
    public Directory getDirectory() {
        return directory;
    }

    private AccountHandle toAccountHandle(SigningKeyPair keyPair, HttpResponse<String> response,
                                          Session session) {
        final var location = Payloads.findLocation(response)
                .orElseThrow(() -> new AcmeClientException("cannot find Location header in the response"));

        final var account = Payloads.deserialize(response.body(), Account.class);

        return RetryableHandle.create(
                session,
                new AccountHandleImpl(httpClient, requestTimout, session, account, keyPair, location),
                AccountHandle.class
        );
    }
}
