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
import java.util.concurrent.atomic.AtomicReference;

import ch.alni.certblues.acme.client.AcmeServerException;
import ch.alni.certblues.acme.client.Authorization;
import ch.alni.certblues.acme.client.AuthorizationHandle;
import ch.alni.certblues.acme.client.SigningKeyPair;
import ch.alni.certblues.acme.jws.JwsObject;

import static org.slf4j.LoggerFactory.getLogger;

class AuthorizationHandleImpl implements AuthorizationHandle {
    private static final Logger LOG = getLogger(AuthorizationHandleImpl.class);

    private final HttpClient httpClient;
    private final Duration requestTimout;
    private final Session session;
    private final AtomicReference<Authorization> authRef;
    private final SigningKeyPair keyPair;
    private final String accountUrl;
    private final String authUrl;

    AuthorizationHandleImpl(HttpClient httpClient,
                            Duration requestTimout,
                            Session session,
                            Authorization auth,
                            SigningKeyPair keyPair,
                            String accountUrl, String authUrl) {
        this.httpClient = httpClient;
        this.requestTimout = requestTimout;
        this.session = session;
        this.authRef = new AtomicReference<>(auth);
        this.keyPair = keyPair;
        this.accountUrl = accountUrl;
        this.authUrl = authUrl;
    }

    @Override
    public Authorization getAuthorization() {
        return authRef.get();
    }

    @Override
    public void provisionChallenge(String type) {
        LOG.info("provisioning the challenge of type {}", type);

        final var nonce = session.getNonce();
        final var challenge = authRef.get().getChallenge(type);

        final JwsObject jwsObject = keyPair.sign(challenge.url(), accountUrl, "{}", nonce);

        final var body = Payloads.serialize(jwsObject);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(challenge.url()))
                .header("Content-Type", "application/jose+json")
                .timeout(requestTimout)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final HttpResponse<String> response = session.executeRequest(
                () -> httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        );

        final int statusCode = response.statusCode();
        if (statusCode == 200) {
            LOG.info("the challenge has been successfully provisioned: {}", response.body());
        }
        else if (statusCode < 400) {
            LOG.warn("unexpected status code {} returned", statusCode);
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
    public String getKeyAuthorization(String type) {
        final var challenge = authRef.get().getChallenge(type);
        return challenge.token() + '.' + keyPair.getPublicKeyThumbprint();
    }

    @Override
    public Authorization reloadAuthorization() {
        LOG.info("reloading the authorization object");

        final var nonce = session.getNonce();

        // POST-as-GET request
        final JwsObject jwsObject = keyPair.sign(authUrl, accountUrl, "", nonce);

        final var body = Payloads.serialize(jwsObject);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authUrl))
                .header("Content-Type", "application/jose+json")
                .timeout(requestTimout)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final HttpResponse<String> response = session.executeRequest(
                () -> httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        );

        final int statusCode = response.statusCode();
        if (statusCode == 200) {
            LOG.info("an authorization update has been successfully returned: {}", response.body());
            return replaceAuth(response);
        }
        else if (statusCode < 400) {
            LOG.warn("unexpected status code {} returned", statusCode);
            return replaceAuth(response);
        }
        else {
            Payloads.extractError(response).ifPresent(
                    error -> {
                        throw new AcmeServerException(error);
                    });

            throw new AcmeServerException(response.body());
        }
    }

    private Authorization replaceAuth(HttpResponse<String> response) {
        final Authorization authorization = Payloads.deserialize(response.body(), Authorization.class);
        authRef.set(authorization);
        return authorization;
    }

}
