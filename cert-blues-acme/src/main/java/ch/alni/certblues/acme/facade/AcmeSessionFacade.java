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

package ch.alni.certblues.acme.facade;

import java.util.stream.Collectors;

import ch.alni.certblues.acme.client.AccountRequest;
import ch.alni.certblues.acme.client.CreatedResource;
import ch.alni.certblues.acme.client.Order;
import ch.alni.certblues.acme.client.OrderFinalizationRequest;
import ch.alni.certblues.acme.client.OrderRequest;
import ch.alni.certblues.acme.client.OrderStatus;
import ch.alni.certblues.acme.key.SigningKeyPair;
import reactor.core.publisher.Mono;

/**
 * Groups together operations with AcmeSession
 */
public class AcmeSessionFacade {
    private final AcmeClient acmeClient;
    private final IdentifierAuthorizationClient authorizationClient;

    public AcmeSessionFacade(AcmeClient acmeClient, IdentifierAuthorizationClient authorizationClient) {
        this.acmeClient = acmeClient;
        this.authorizationClient = authorizationClient;
    }

    /**
     * Creates a new order on the ACME server accessed by the given directory URL.
     *
     * @param accountKeyPair the URL of the directory on the ACME server
     * @param orderRequest   the request to create a new order
     */
    public Mono<CreatedResource<Order>> createOrder(SigningKeyPair accountKeyPair, OrderRequest orderRequest) {
        final var accountRequest = AccountRequest.builder().termsOfServiceAgreed(true).build();
        final var session = acmeClient.login(accountKeyPair, accountRequest);

        final Mono<CreatedResource<Order>> orderResourceMono = session.createOrder(orderRequest).share();
        final Mono<Void> authorizationMono = orderResourceMono.map(resource ->
                        resource.getResource().authorizations().stream()
                                .map(authorizationUrl ->
                                        session.getAuthorization(authorizationUrl)
                                                .flatMap(authorizationClient::process)
                                                .flatMap(challenge -> session.submitChallenge(challenge.url())))
                                .collect(Collectors.toList()))
                .flatMap(Mono::when);

        return authorizationMono.then(orderResourceMono);
    }

    public Mono<Order> submitCsr(SigningKeyPair accountKeyPair, String orderUrl, String encodedCsr) {
        final var accountRequest = AccountRequest.builder()
                .onlyReturnExisting(true).termsOfServiceAgreed(true)
                .build();

        final var session = acmeClient.login(accountKeyPair, accountRequest);

        final var finalizationRequest = OrderFinalizationRequest.builder().csr(encodedCsr).build();

        return session.getOrder(orderUrl)
                .flatMap(order -> order.status() == OrderStatus.READY ?
                        session.finalizeOrder(order.finalizeUrl(), finalizationRequest) : Mono.just(order));
    }

    public Mono<String> downloadCertificate(SigningKeyPair accountKeyPair, String certificateUrl) {
        final var accountRequest = AccountRequest.builder()
                .onlyReturnExisting(true).termsOfServiceAgreed(true)
                .build();
        final var session = acmeClient.login(accountKeyPair, accountRequest);

        return session.downloadCertificate(certificateUrl);
    }
}
