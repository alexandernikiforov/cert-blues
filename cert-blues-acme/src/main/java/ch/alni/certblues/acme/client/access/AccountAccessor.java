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

package ch.alni.certblues.acme.client.access;

import ch.alni.certblues.acme.client.request.CreatedResource;
import ch.alni.certblues.acme.client.request.NonceSource;
import ch.alni.certblues.acme.client.request.RequestHandler;
import ch.alni.certblues.acme.protocol.Account;
import ch.alni.certblues.acme.protocol.AccountRequest;
import reactor.core.publisher.Mono;

public class AccountAccessor {

    private final NonceSource nonceSource;
    private final PayloadSigner payloadSigner;
    private final RetryHandler retryHandler;
    private final RequestHandler requestHandler;

    public AccountAccessor(NonceSource nonceSource,
                           PayloadSigner payloadSigner,
                           RetryHandler retryHandler,
                           RequestHandler requestHandler) {
        this.nonceSource = nonceSource;
        this.payloadSigner = payloadSigner;
        this.retryHandler = retryHandler;
        this.requestHandler = requestHandler;
    }

    /**
     * Creates a new ore returns an existing account.
     *
     * @param newAccountUrl   "new-account" URL of the ACME directory
     * @param resourceRequest request handling details of how to get the account
     */
    public Mono<? extends CreatedResource<Account>> getAccount(String newAccountUrl, AccountRequest resourceRequest) {
        return nonceSource.getNonce()
                .flatMap(nonce -> payloadSigner.sign(newAccountUrl, resourceRequest, nonce))
                .flatMap(jwsObject -> requestHandler.create(newAccountUrl, jwsObject, nonceSource, Account.class))
                .retryWhen(retryHandler.getRetry());
    }

    /**
     * Returns account object with a GET-as-POST request to the given URL.
     *
     * @param accountUrl URL of the account to return
     */
    public Mono<Account> getAccount(String accountUrl) {
        return nonceSource.getNonce()
                .flatMap(nonce -> payloadSigner.sign(accountUrl, accountUrl, "", nonce))
                .flatMap(jwsObject -> requestHandler.request(accountUrl, jwsObject, nonceSource, Account.class))
                .retryWhen(retryHandler.getRetry());
    }
}
