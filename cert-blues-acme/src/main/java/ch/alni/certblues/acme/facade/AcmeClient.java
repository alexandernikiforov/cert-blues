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
import ch.alni.certblues.acme.client.Directory;
import ch.alni.certblues.acme.client.Order;
import ch.alni.certblues.acme.client.OrderRequest;
import ch.alni.certblues.acme.client.access.AccountAccessor;
import ch.alni.certblues.acme.client.access.AuthorizationAccessor;
import ch.alni.certblues.acme.client.access.ChallengeAccessor;
import ch.alni.certblues.acme.client.access.ChallengeProvisioner;
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
 * Client of the ACME server.
 */
public class AcmeClient {

    private final SigningKeyPair accountKeyPair;

    private final RetryHandler retryHandler = new RetryHandler(5);
    private final RequestHandler requestHandler;
    private final PayloadSigner payloadSigner;
    private final ChallengeProvisioner challengeProvisioner;

    /**
     * Creates a new instance.
     *
     * @param httpClient           the HTTP client to use
     * @param accountKeyPair       the key pair identifying the account on the ACME server
     * @param challengeProvisioner the interface to the system provisioning the authorization challenges
     */
    public AcmeClient(HttpClient httpClient, SigningKeyPair accountKeyPair, ChallengeProvisioner challengeProvisioner) {
        this.accountKeyPair = accountKeyPair;
        this.requestHandler = new RequestHandler(httpClient);
        this.payloadSigner = new PayloadSigner(accountKeyPair);
        this.challengeProvisioner = challengeProvisioner;
    }

    /**
     * Creates a new order on the ACME server accessed by the given directory URL.
     *
     * @param directoryUrl the URL of the directory on the ACME server
     * @param orderRequest the request to create a new order
     */
    public Mono<CreatedResource<Order>> createOrder(String directoryUrl, OrderRequest orderRequest) {
        final var nonceSource = new NonceSource();
        final var directoryAccessor = new DirectoryAccessor(requestHandler, directoryUrl);
        final var accountAccessor = new AccountAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);
        final var orderAccessor = new OrderAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);
        final var authorizationAccessor = new AuthorizationAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);
        final var challengeAccessor = new ChallengeAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);
        final var authorizationHandler = new IdentifierAuthorizationClient(accountKeyPair, challengeProvisioner);

        final AccountRequest accountRequest = AccountRequest.builder().termsOfServiceAgreed(true).build();

        // let's build the request to get the tuple (directory, account, accountUrl)
        final Mono<Directory> directoryMono = directoryAccessor.getDirectory()
                .doOnNext(directory -> requestHandler.updateNonce(directory.newNonce(), nonceSource))
                .share();
        final var accountResourceMono = directoryMono
                .flatMap(directory -> accountAccessor.getAccount(directory.newAccount(), accountRequest))
                .share();
        final var accountUrlMono = accountResourceMono.map(CreatedResource::getResourceUrl)
                .share();
        final var orderMono = Mono.zip(directoryMono, accountUrlMono)
                .flatMap(tuple -> orderAccessor.createOrder(tuple.getT2(), tuple.getT1().newOrder(), orderRequest))
                .share();

        final Mono<Void> authorizationMono = Mono.zip(accountUrlMono, orderMono.map(CreatedResource::getResource))
                // create requests to get the authorizations for each of the authorization URL in the order
                .map(tuple -> tuple.getT2().authorizations().stream().map(authorizationUrl -> authorizationAccessor
                                .getAuthorization(tuple.getT1(), authorizationUrl)
                                .flatMap(authorizationHandler::process)
                                .flatMap(challenge -> challengeAccessor.submitChallenge(tuple.getT1(), challenge.url()))
                        )
                        .collect(Collectors.toList()))
                // for each challenge mono try to execute the authorization requests
                .flatMap(Mono::when);

        // submit request for authorizations
        return authorizationMono.then(Mono.zip(accountUrlMono, orderMono)
                .flatMap(tuple -> orderAccessor.getOrder(tuple.getT1(), tuple.getT2().getResourceUrl())
                        .map(order -> new CreatedResource<>(order, tuple.getT2().getResourceUrl()))
                ));
    }
}
