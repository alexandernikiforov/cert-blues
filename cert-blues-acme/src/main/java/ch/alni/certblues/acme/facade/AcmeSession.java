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

import ch.alni.certblues.acme.client.Account;
import ch.alni.certblues.acme.client.AccountRequest;
import ch.alni.certblues.acme.client.Authorization;
import ch.alni.certblues.acme.client.Challenge;
import ch.alni.certblues.acme.client.CreatedResource;
import ch.alni.certblues.acme.client.Directory;
import ch.alni.certblues.acme.client.Order;
import ch.alni.certblues.acme.client.OrderFinalizationRequest;
import ch.alni.certblues.acme.client.OrderRequest;
import ch.alni.certblues.acme.client.access.AccountAccessor;
import ch.alni.certblues.acme.client.access.AuthorizationAccessor;
import ch.alni.certblues.acme.client.access.ChallengeAccessor;
import ch.alni.certblues.acme.client.access.DirectoryAccessor;
import ch.alni.certblues.acme.client.access.OrderAccessor;
import ch.alni.certblues.acme.client.access.PayloadSigner;
import ch.alni.certblues.acme.client.access.RetryHandler;
import ch.alni.certblues.acme.client.request.NonceSource;
import ch.alni.certblues.acme.client.request.RequestHandler;
import ch.alni.certblues.acme.key.SigningKeyPair;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Client session against the ACME server.
 */
public class AcmeSession {

    private final SigningKeyPair accountKeyPair;

    private final RetryHandler retryHandler = new RetryHandler(5);
    private final RequestHandler requestHandler;
    private final PayloadSigner payloadSigner;

    private final AccountAccessor accountAccessor;
    private final OrderAccessor orderAccessor;
    private final AuthorizationAccessor authorizationAccessor;
    private final ChallengeAccessor challengeAccessor;

    private final Mono<Directory> directoryMono;
    private final Mono<String> accountUrlMono;
    private final Mono<Account> accountMono;

    /**
     * Creates a new instance.
     *
     * @param httpClient     the HTTP client to use
     * @param nonceSource    the source for of nonce values
     * @param accountKeyPair the key pair identifying the account on the ACME server
     * @param directoryUrl   URL of the directory on ACME server
     * @param accountRequest request to create or retrieve the account
     */
    AcmeSession(HttpClient httpClient, NonceSource nonceSource, SigningKeyPair accountKeyPair, String directoryUrl, AccountRequest accountRequest) {
        this.accountKeyPair = accountKeyPair;
        this.requestHandler = new RequestHandler(httpClient);
        this.payloadSigner = new PayloadSigner(accountKeyPair);

        this.accountAccessor = new AccountAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);
        this.orderAccessor = new OrderAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);
        this.authorizationAccessor = new AuthorizationAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);
        this.challengeAccessor = new ChallengeAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);

        // pre-build the basis mono's
        final var directoryAccessor = new DirectoryAccessor(requestHandler, directoryUrl);
        directoryMono = directoryAccessor.getDirectory()
                .doOnNext(directory -> requestHandler.updateNonce(directory.newNonce(), nonceSource))
                .share();

        final var accountResourceMono = directoryMono
                .flatMap(directory -> accountAccessor.getAccount(directory.newAccount(), accountRequest));
        accountUrlMono = accountResourceMono.map(CreatedResource::getResourceUrl).share();
        accountMono = accountResourceMono.map(CreatedResource::getResource).share();
    }

    public Mono<Account> getAccount() {
        return accountMono;
    }

    public Mono<CreatedResource<Order>> createOrder(OrderRequest orderRequest) {
        return Mono.zip(directoryMono, accountUrlMono).flatMap(tuple ->
                orderAccessor.createOrder(tuple.getT2(), tuple.getT1().newOrder(), orderRequest)
        );
    }

    public Mono<Order> getOrder(String orderUrl) {
        return accountUrlMono.flatMap(accountUrl -> orderAccessor.getOrder(accountUrl, orderUrl));
    }

    public Mono<Authorization> getAuthorization(String authorizationUrl) {
        return accountUrlMono.flatMap(accountUrl ->
                authorizationAccessor.getAuthorization(accountUrl, authorizationUrl)
        );
    }

    public Mono<Challenge> submitChallenge(String challengeUrl) {
        return accountUrlMono.flatMap(accountUrl -> challengeAccessor.submitChallenge(accountUrl, challengeUrl));
    }

    public Mono<Order> finalizeOrder(String finalizeUrl, OrderFinalizationRequest request) {
        return accountUrlMono.flatMap(accountUrl -> orderAccessor.submitCsr(accountUrl, finalizeUrl, request));
    }

    public Mono<String> downloadCertificate(String certificateUrl) {
        return accountUrlMono.flatMap(accountUrl -> orderAccessor.downloadCertificate(accountUrl, certificateUrl));
    }
}
