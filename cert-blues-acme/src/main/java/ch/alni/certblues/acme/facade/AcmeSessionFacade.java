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

import java.util.List;
import java.util.stream.Collectors;

import ch.alni.certblues.acme.client.AccountRequest;
import ch.alni.certblues.acme.client.Challenge;
import ch.alni.certblues.acme.client.CreatedResource;
import ch.alni.certblues.acme.client.Order;
import ch.alni.certblues.acme.client.OrderRequest;
import ch.alni.certblues.acme.client.OrderStatus;
import ch.alni.certblues.acme.client.access.DnsChallengeProvisioner;
import ch.alni.certblues.acme.client.access.HttpChallengeProvisioner;
import ch.alni.certblues.acme.key.CertificateEntry;
import ch.alni.certblues.acme.key.SigningKeyPair;
import reactor.core.publisher.Mono;

/**
 * Groups together operations with AcmeSession. Each method call on this facade is a separate login establishing a new
 * "session". A session assumes using the same account key to talk to the same ACME server with the same client.
 */
public class AcmeSessionFacade {

    private final AcmeClient acmeClient;
    private final SigningKeyPair accountKeyPair;

    public AcmeSessionFacade(AcmeClient acmeClient, SigningKeyPair accountKeyPair) {
        this.acmeClient = acmeClient;
        this.accountKeyPair = accountKeyPair;
    }

    /**
     * Creates and provisions a new order on the ACME server.
     *
     * @param orderRequest the request to create a new order
     * @return a wrapper over the created order resource and provisioned challenges
     */
    public Mono<ProvisionedOrder> provisionOrder(OrderRequest orderRequest,
                                                 DnsChallengeProvisioner dnsChallengeProvisioner,
                                                 HttpChallengeProvisioner httpChallengeProvisioner) {
        final var accountRequest = AccountRequest.builder().termsOfServiceAgreed(true).build();
        final var session = acmeClient.login(accountKeyPair, accountRequest);

        final Mono<CreatedResource<Order>> orderResourceMono = session.createOrder(orderRequest).share();
        final Mono<List<Challenge>> authorizationMono = orderResourceMono
                .map(resource -> resource.getResource().authorizations())
                .flatMap(authorizationUrls -> session.provision(
                        authorizationUrls, dnsChallengeProvisioner, httpChallengeProvisioner
                ));

        return Mono.zip(orderResourceMono, authorizationMono, ProvisionedOrder::create);
    }

    /**
     * Submits the given challenges for the order identified by the given order URL.
     *
     * @param orderUrl   URL of the order to submit the challenges for
     * @param challenges the challenges to submit
     * @return mono over the submitted challenges
     */
    public Mono<List<Challenge>> submitChallenges(String orderUrl, List<Challenge> challenges) {
        final var accountRequest = AccountRequest.builder()
                .onlyReturnExisting(true).termsOfServiceAgreed(true)
                .build();

        final var session = acmeClient.login(accountKeyPair, accountRequest);

        final var challengeUrls = challenges.stream().map(Challenge::url).collect(Collectors.toList());

        final var submitChallengeMono = session.submitChallenges(challengeUrls);
        final var getChallengesMono = session.getChallenges(challengeUrls);
        final var orderMono = session.getOrder(orderUrl);

        // if the order is pending submit challenges, otherwise try to re-read them from the server
        return orderMono.flatMap(order -> order.status() == OrderStatus.PENDING ?
                submitChallengeMono : getChallengesMono);
    }

    /**
     * Checks the order and submits a CSR if it is ready or downloads a certificate if the certificate is ready.
     *
     * @param orderUrl         URL of the order to submit the challenges for
     * @param certificateEntry the certificate entry to use for sumbitting CSR
     * @return mono that just completes or returns a certificate if it is ready
     */
    public Mono<Order> checkOrder(String orderUrl, CertificateEntry certificateEntry) {
        final var accountRequest = AccountRequest.builder()
                .onlyReturnExisting(true).termsOfServiceAgreed(true)
                .build();

        final var session = acmeClient.login(accountKeyPair, accountRequest);

        return session.getOrder(orderUrl)
                .flatMap(order -> finalizeOrDownloadCertificateIfReady(session, order, certificateEntry));
    }

    /**
     * Downloads the certificate from the given URL.
     *
     * @param certificateUrl the URL of the certificate
     * @return mono over the certificate in PEM format
     */
    public Mono<String> downloadCertificate(String certificateUrl) {
        final var accountRequest = AccountRequest.builder()
                .onlyReturnExisting(true).termsOfServiceAgreed(true)
                .build();
        final var session = acmeClient.login(accountKeyPair, accountRequest);

        return session.downloadCertificate(certificateUrl);
    }

    private Mono<Order> finalizeOrDownloadCertificateIfReady(AcmeSession session, Order order, CertificateEntry certificateEntry) {
        switch (order.status()) {
            case INVALID:
                return Mono.error(new IllegalStateException("invalid order: " + order));
            case READY:
                return session.finalizeOrder(order.finalizeUrl(), certificateEntry);
            case VALID:
                return session.downloadCertificate(order.certificate())
                        .flatMap(certificateEntry::upload)
                        .then(Mono.just(order));
            default:
                return Mono.empty();
        }
    }
}
