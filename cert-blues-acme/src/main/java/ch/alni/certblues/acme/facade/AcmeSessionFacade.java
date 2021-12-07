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
import ch.alni.certblues.acme.client.OrderFinalizationRequest;
import ch.alni.certblues.acme.client.OrderRequest;
import ch.alni.certblues.acme.client.OrderStatus;
import ch.alni.certblues.acme.client.access.DnsChallengeProvisioner;
import ch.alni.certblues.acme.client.access.HttpChallengeProvisioner;
import ch.alni.certblues.acme.key.SigningKeyPair;
import reactor.core.publisher.Mono;

/**
 * Groups together operations with AcmeSession
 */
public class AcmeSessionFacade {
    private final AcmeClient acmeClient;
    private final DnsChallengeProvisioner dnsChallengeProvisioner;
    private final HttpChallengeProvisioner httpChallengeProvisioner;

    public AcmeSessionFacade(AcmeClient acmeClient,
                             DnsChallengeProvisioner dnsChallengeProvisioner,
                             HttpChallengeProvisioner httpChallengeProvisioner) {
        this.acmeClient = acmeClient;
        this.dnsChallengeProvisioner = dnsChallengeProvisioner;
        this.httpChallengeProvisioner = httpChallengeProvisioner;
    }

    /**
     * Creates and provisions a new order on the ACME server.
     *
     * @param accountKeyPair the key pair of the account on the ACME server
     * @param orderRequest   the request to create a new order
     * @return a wrapper over the created order resource and provisioned challenges
     */
    public Mono<ProvisionedOrder> provisionOrder(SigningKeyPair accountKeyPair, OrderRequest orderRequest) {
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
     * Submit the given challenges for the order identified by the given order URL.
     *
     * @param accountKeyPair the key pair of the account on the ACME server
     * @param orderUrl       URL of the order to subnit the challenges for
     * @param challenges     the challenges to submit
     * @return mono over the submitted challenges
     */
    public Mono<List<Challenge>> submitChallenges(SigningKeyPair accountKeyPair, String orderUrl, List<Challenge> challenges) {
        final var accountRequest = AccountRequest.builder()
                .onlyReturnExisting(true).termsOfServiceAgreed(true)
                .build();

        final var session = acmeClient.login(accountKeyPair, accountRequest);

        final var challengeMonoList = challenges.stream()
                .map(Challenge::url)
                .map(session::submitChallenge)
                .collect(Collectors.toList());

        final Mono<List<Challenge>> challengesMono = Mono.zip(challengeMonoList, List::of)
                .map(objects -> objects.stream().map(Challenge.class::cast).collect(Collectors.toList()));

        final Mono<Order> orderMono = session.getOrder(orderUrl);

        return orderMono.flatMap(order -> order.status() == OrderStatus.PENDING ? challengesMono :
                Mono.error(new IllegalStateException("order must be pending to submit challenges: " + order)));
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
