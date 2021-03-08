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

import com.google.common.base.Preconditions;

import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import ch.alni.certblues.acme.cert.CertificateSigningRequest;
import ch.alni.certblues.acme.cert.CertificateSigningRequests;
import ch.alni.certblues.acme.client.AcmeServerException;
import ch.alni.certblues.acme.client.Authorization;
import ch.alni.certblues.acme.client.AuthorizationHandle;
import ch.alni.certblues.acme.client.Order;
import ch.alni.certblues.acme.client.OrderFinalizationRequest;
import ch.alni.certblues.acme.client.OrderHandle;
import ch.alni.certblues.acme.client.SigningKeyPair;
import ch.alni.certblues.acme.jws.JwsObject;

import static org.slf4j.LoggerFactory.getLogger;

class OrderHandleImpl implements OrderHandle {
    private static final Logger LOG = getLogger(OrderHandleImpl.class);

    private final HttpClient httpClient;
    private final Duration requestTimout;
    private final Session session;
    private final AtomicReference<Order> orderRef;
    private final SigningKeyPair keyPair;
    private final String accountUrl;
    private final String orderUrl;

    OrderHandleImpl(HttpClient httpClient,
                    Duration requestTimout,
                    Session session,
                    Order order,
                    SigningKeyPair keyPair,
                    String accountUrl,
                    String orderUrl) {

        this.httpClient = httpClient;
        this.requestTimout = requestTimout;
        this.session = session;
        this.orderRef = new AtomicReference<>(order);
        this.keyPair = keyPair;
        this.accountUrl = accountUrl;
        this.orderUrl = orderUrl;
    }

    @Override
    public Order getOrder() {
        return orderRef.get();
    }

    @Override
    public Order reloadOrder() {
        LOG.info("retrieving the order information");

        final var nonce = session.getNonce();

        // POST-as-GET request
        final JwsObject jwsObject = keyPair.sign(orderUrl, accountUrl, "", nonce);

        final var body = Payloads.serialize(jwsObject);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(orderUrl))
                .header("Content-Type", "application/jose+json")
                .timeout(requestTimout)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final HttpResponse<String> response = session.executeRequest(
                () -> httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        );

        final int statusCode = response.statusCode();
        if (statusCode == 200) {
            LOG.info("an order update has been successfully returned: {}", response.body());
            return replaceOrder(response);
        }
        else if (statusCode < 400) {
            LOG.warn("unexpected status code {} returned", statusCode);
            return replaceOrder(response);
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
    public List<AuthorizationHandle> getAuthorizations() {
        LOG.info("retrieving authorization");
        final var order = orderRef.get();
        return order.authorizations().stream().map(this::doGetAuthorization).collect(Collectors.toList());
    }

    @Override
    public Order finalizeOrder(CertificateSigningRequest csr) {
        LOG.info("finalizing the certificate order for CSR {}", csr);

        final var nonce = session.getNonce();
        final var order = orderRef.get();
        final var finalizationRequest = OrderFinalizationRequest.builder()
                .csr(CertificateSigningRequests.toUrlEncodedString(csr))
                .build();

        final JwsObject jwsObject = keyPair.sign(order.finalizeUrl(), accountUrl, finalizationRequest, nonce);

        final var body = Payloads.serialize(jwsObject);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(order.finalizeUrl()))
                .header("Content-Type", "application/jose+json")
                .timeout(requestTimout)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final HttpResponse<String> response = session.executeRequest(
                () -> httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        );

        final int statusCode = response.statusCode();
        if (statusCode == 200) {
            LOG.info("order finalization request accepted: {}", response.body());
            return replaceOrder(response);
        }
        else if (statusCode < 400) {
            LOG.warn("unexpected status code {} returned", statusCode);
            return replaceOrder(response);
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
    public String downloadCertificate() {
        LOG.info("downloading the certificate");
        final Order order = orderRef.get();

        Preconditions.checkState(order.certificate() != null, "certificate is not yet ready");

        final var nonce = session.getNonce();

        // POST-as-GET request
        final JwsObject jwsObject = keyPair.sign(order.certificate(), accountUrl, "", nonce);

        final var body = Payloads.serialize(jwsObject);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(order.certificate()))
                .header("Content-Type", "application/jose+json")
                .timeout(requestTimout)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final HttpResponse<String> response = session.executeRequest(
                () -> httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        );

        final int statusCode = response.statusCode();
        if (statusCode == 200) {
            LOG.info("a new certificate has been successfully returned: {}", response.body());
            return response.body();
        }
        else if (statusCode < 400) {
            LOG.warn("unexpected status code {} returned", statusCode);
            return response.body();
        }
        else {
            Payloads.extractError(response).ifPresent(
                    error -> {
                        throw new AcmeServerException(error);
                    });

            throw new AcmeServerException(response.body());
        }
    }

    private AuthorizationHandle doGetAuthorization(String authorizationUrl) {
        LOG.info("retrieving authorization for URL {}", authorizationUrl);
        final var nonce = session.getNonce();

        // POST-as-GET request
        final JwsObject jwsObject = keyPair.sign(authorizationUrl, accountUrl, "", nonce);

        final var body = Payloads.serialize(jwsObject);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authorizationUrl))
                .header("Content-Type", "application/jose+json")
                .timeout(requestTimout)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final HttpResponse<String> response = session.executeRequest(
                () -> httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        );

        final int statusCode = response.statusCode();
        if (statusCode == 200) {
            LOG.info("an authorization object has been successfully returned: {}", response.body());
            return toAuthorizationHandle(response, authorizationUrl);
        }
        else if (statusCode < 400) {
            LOG.warn("unexpected status code {} returned", statusCode);
            return toAuthorizationHandle(response, authorizationUrl);
        }
        else {
            Payloads.extractError(response).ifPresent(
                    error -> {
                        throw new AcmeServerException(error);
                    });

            throw new AcmeServerException(response.body());
        }
    }

    private AuthorizationHandle toAuthorizationHandle(HttpResponse<String> response, String authorizationUrl) {
        final Authorization authorization = Payloads.deserialize(response.body(), Authorization.class);

        return RetryableHandle.create(
                session,
                new AuthorizationHandleImpl(httpClient, requestTimout, session, authorization, keyPair, accountUrl, authorizationUrl),
                AuthorizationHandle.class
        );
    }

    private Order replaceOrder(HttpResponse<String> response) {
        final Order order = Payloads.deserialize(response.body(), Order.class);
        orderRef.set(order);
        return order;
    }
}
