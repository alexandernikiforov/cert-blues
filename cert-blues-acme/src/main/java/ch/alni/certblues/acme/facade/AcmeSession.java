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

import ch.alni.certblues.acme.client.access.AccountAccessor;
import ch.alni.certblues.acme.client.access.AuthorizationAccessor;
import ch.alni.certblues.acme.client.access.ChallengeAccessor;
import ch.alni.certblues.acme.client.access.OrderAccessor;
import ch.alni.certblues.acme.client.access.PayloadSigner;
import ch.alni.certblues.acme.client.access.RetryHandler;
import ch.alni.certblues.acme.client.request.CreatedResource;
import ch.alni.certblues.acme.client.request.NonceSource;
import ch.alni.certblues.acme.client.request.RequestHandler;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.acme.protocol.Account;
import ch.alni.certblues.acme.protocol.AccountRequest;
import ch.alni.certblues.acme.protocol.Authorization;
import ch.alni.certblues.acme.protocol.Challenge;
import ch.alni.certblues.acme.protocol.ChallengeStatus;
import ch.alni.certblues.acme.protocol.Directory;
import ch.alni.certblues.acme.protocol.Order;
import ch.alni.certblues.acme.protocol.OrderFinalizationRequest;
import ch.alni.certblues.acme.protocol.OrderRequest;
import reactor.core.publisher.Mono;

/**
 * Client session against the ACME server.
 */
public class AcmeSession {

    private final AccountAccessor accountAccessor;
    private final OrderAccessor orderAccessor;
    private final AuthorizationAccessor authorizationAccessor;
    private final ChallengeAccessor challengeAccessor;

    private final Mono<Directory> directoryMono;
    private final Mono<String> accountUrlMono;
    private final Mono<Account> accountMono;
    private final Mono<String> publicKeyThumbprintMono;

    /**
     * Creates a new instance.
     *
     * @param requestHandler interface to handle HTTP requests
     * @param accountKeyPair the key pair identifying the account on the ACME server
     * @param directoryMono  how to get the directory information from ACME server
     * @param accountRequest request to create or retrieve the account
     */
    AcmeSession(RequestHandler requestHandler, Mono<Directory> directoryMono, SigningKeyPair accountKeyPair, AccountRequest accountRequest) {
        final var payloadSigner = new PayloadSigner(accountKeyPair);
        final var retryHandler = new RetryHandler(5);

        // we create one nonce source per session
        final var nonceSource = new NonceSource(directoryMono.map(Directory::newNonce).flatMap(requestHandler::getNonce));

        this.accountAccessor = new AccountAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);
        this.orderAccessor = new OrderAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);
        this.authorizationAccessor = new AuthorizationAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);
        this.challengeAccessor = new ChallengeAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);

        this.directoryMono = directoryMono;

        // pre-build the base mono's
        final var accountResourceMono = directoryMono
                .flatMap(directory -> accountAccessor.getAccount(directory.newAccount(), accountRequest));
        accountUrlMono = accountResourceMono.map(CreatedResource::getResourceUrl).share();
        accountMono = accountResourceMono.map(CreatedResource::getResource).share();
        publicKeyThumbprintMono = accountKeyPair.getPublicKeyThumbprint().share();
    }

    public synchronized Mono<Account> getAccount() {
        return accountMono;
    }

    /**
     * Returns the thumbprint of the account key used in this session.
     */
    public synchronized Mono<String> getPublicKeyThumbprint() {
        return publicKeyThumbprintMono;
    }

    public synchronized Mono<CreatedResource<Order>> createOrder(OrderRequest orderRequest) {
        return Mono.zip(directoryMono, accountUrlMono).flatMap(tuple ->
                orderAccessor.createOrder(tuple.getT2(), tuple.getT1().newOrder(), orderRequest)
        );
    }

    /**
     * Return order object by the given order URL.
     */
    public synchronized Mono<Order> getOrder(String orderUrl) {
        return accountUrlMono.flatMap(accountUrl -> orderAccessor.getOrder(accountUrl, orderUrl));
    }

    public Mono<Authorization> getAuthorization(String authorizationUrl) {
        return accountUrlMono.flatMap(accountUrl ->
                authorizationAccessor.getAuthorization(accountUrl, authorizationUrl)
        );
    }

    /**
     * Provisions the challenges of the authorizations identified by the given URLs.
     *
     * @return list of provisioned challenges (as mono)
     */
    public Mono<List<Challenge>> provision(List<String> authorizationUrls,
                                           AuthorizationProvisioner authorizationProvisioner) {

        return accountUrlMono.map(accountUrl ->
                        authorizationUrls.stream()
                                .map(authorizationUrl -> authorizationAccessor
                                        .getAuthorization(accountUrl, authorizationUrl)
                                        .flatMap(authorizationProvisioner::process))
                                .collect(Collectors.toList()))
                // zip waits for all challenge provisions to complete
                .flatMap(monoList -> Mono.zip(monoList, List::of))
                // cast back to Challenge objects
                .map(objects -> objects.stream().map(Challenge.class::cast).collect(Collectors.toList()));
    }

    /**
     * Submit challenge identified by the given URL.
     *
     * @param challenge submitted challenge or the same challenge if its status is not PENDING
     */
    public Mono<Challenge> submitChallenge(Challenge challenge) {
        if (challenge.status() == ChallengeStatus.PENDING) {
            return accountUrlMono.flatMap(accountUrl -> challengeAccessor.submitChallenge(accountUrl, challenge.url()));
        }
        else {
            return Mono.just(challenge);
        }
    }

    /**
     * Submits the following challenges identified by their respective URLs.
     *
     * @param challenges URLs pointing to the
     * @return mono of the list of submitted challenges
     */
    public Mono<List<Challenge>> submitChallenges(List<Challenge> challenges) {
        final var challengeMonoList = challenges.stream()
                .map(this::submitChallenge)
                .collect(Collectors.toList());

        return Mono.zip(challengeMonoList, List::of)
                .map(objects -> objects.stream().map(Challenge.class::cast).collect(Collectors.toList()));
    }

    public Mono<Challenge> getChallenge(String challengeUrl) {
        return accountUrlMono.flatMap(accountUrl -> challengeAccessor.getChallenge(accountUrl, challengeUrl));
    }

    /**
     * Returns the status of the following challenges identified by their respective URLs.
     *
     * @param challengeUrls URLs pointing to the
     * @return mono of the list of challenges
     */
    public Mono<List<Challenge>> getChallenges(List<String> challengeUrls) {
        final var challengeMonoList = challengeUrls.stream()
                .map(this::getChallenge)
                .collect(Collectors.toList());

        return Mono.zip(challengeMonoList, List::of)
                .map(objects -> objects.stream().map(Challenge.class::cast).collect(Collectors.toList()));
    }

    /**
     * Finalizes the order by submitting a CSR from the given certificate entry.
     *
     * @param finalizeUrl              the URL to submit the CSR
     * @param orderFinalizationRequest the certificate entry to be used to create the CSR
     * @return mono over the latest state of the order
     */
    public Mono<Order> finalizeOrder(String finalizeUrl, OrderFinalizationRequest orderFinalizationRequest) {
        return accountUrlMono
                .flatMap(accountUrl -> orderAccessor.submitCsr(accountUrl, finalizeUrl, orderFinalizationRequest));
    }

    public Mono<String> downloadCertificate(String certificateUrl) {
        return accountUrlMono.flatMap(accountUrl -> orderAccessor.downloadCertificate(accountUrl, certificateUrl));
    }
}