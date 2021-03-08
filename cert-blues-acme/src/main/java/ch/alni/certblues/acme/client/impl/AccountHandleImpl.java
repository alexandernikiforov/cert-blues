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

import ch.alni.certblues.acme.client.Account;
import ch.alni.certblues.acme.client.AccountDeactivationRequest;
import ch.alni.certblues.acme.client.AccountHandle;
import ch.alni.certblues.acme.client.AccountRequest;
import ch.alni.certblues.acme.client.AcmeClientException;
import ch.alni.certblues.acme.client.AcmeServerException;
import ch.alni.certblues.acme.client.Order;
import ch.alni.certblues.acme.client.OrderHandle;
import ch.alni.certblues.acme.client.OrderRequest;
import ch.alni.certblues.acme.client.SigningKeyPair;
import ch.alni.certblues.acme.jws.JwsObject;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the interface to work with ACME accounts.
 */
class AccountHandleImpl implements AccountHandle {
    private static final Logger LOG = getLogger(AccountHandleImpl.class);

    private final HttpClient httpClient;
    private final Duration requestTimout;
    private final Session session;
    private final AtomicReference<Account> accountRef;
    private final SigningKeyPair keyPair;
    private final String accountUrl;

    AccountHandleImpl(HttpClient httpClient,
                      Duration requestTimout,
                      Session session, Account account,
                      SigningKeyPair keyPair,
                      String accountUrl) {
        this.httpClient = httpClient;
        this.requestTimout = requestTimout;
        this.session = session;
        this.accountRef = new AtomicReference<>(account);
        this.keyPair = keyPair;
        this.accountUrl = accountUrl;
    }

    @Override
    public Account getAccount() {
        LOG.info("returning the latest account information");
        return accountRef.get();
    }

    @Override
    public void deactivateAccount() {
        LOG.info("deactivating the current account {}", accountUrl);

        final var nonce = session.getNonce();

        final JwsObject jwsObject = keyPair.sign(accountUrl, accountUrl,
                AccountDeactivationRequest.builder().build(), nonce);

        final var body = Payloads.serialize(jwsObject);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(accountUrl))
                .header("Content-Type", "application/jose+json")
                .timeout(requestTimout)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final HttpResponse<String> response = session.executeRequest(
                () -> httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        );

        final int statusCode = response.statusCode();
        if (statusCode == 200) {
            LOG.info("account {} has been deactivated", accountUrl);
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
    public OrderHandle createOrder(OrderRequest orderRequest) {
        LOG.info("creating a new order with parameters {}", orderRequest);

        final var directory = session.getDirectory();
        final var orderUrl = directory.newOrder();
        final var nonce = session.getNonce();

        final JwsObject jwsObject = keyPair.sign(orderUrl, accountUrl, orderRequest, nonce);

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
        if (statusCode == 201) {
            LOG.info("a new order has been created {}", response.body());
            return toOrderHandle(response);
        }
        else if (statusCode < 400) {
            LOG.warn("unexpected status code {} returned", statusCode);
            return toOrderHandle(response);
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
    public Account reloadAccount() {
        LOG.info("retrieving the account information");

        final var directory = session.getDirectory();
        final var newAccountUrl = directory.newAccount();
        final var accountRequest = AccountRequest.builder().onlyReturnExisting(true).build();
        final var nonce = session.getNonce();

        final JwsObject jwsObject = keyPair.sign(newAccountUrl, accountRequest, nonce);

        final var body = Payloads.serialize(jwsObject);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(newAccountUrl))
                .header("Content-Type", "application/jose+json")
                .timeout(requestTimout)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final HttpResponse<String> response = session.executeRequest(
                () -> httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        );

        final int statusCode = response.statusCode();
        if (statusCode == 200) {
            LOG.info("an existing account has been successfully returned: {}", response.body());
            return replaceAccount(response);
        }
        else if (statusCode < 400) {
            LOG.warn("unexpected status code {} returned", statusCode);
            return replaceAccount(response);
        }
        else {
            Payloads.extractError(response).ifPresent(
                    error -> {
                        throw new AcmeServerException(error);
                    });

            throw new AcmeServerException(response.body());
        }
    }

    private Account replaceAccount(HttpResponse<String> response) {
        final Account account = Payloads.deserialize(response.body(), Account.class);
        accountRef.set(account);
        return account;
    }

    private OrderHandle toOrderHandle(HttpResponse<String> response) {
        final Order order = Payloads.deserialize(response.body(), Order.class);
        final String orderUrl = Payloads.findLocation(response)
                .orElseThrow(() -> new AcmeClientException("cannot find Location header in the response"));

        return RetryableHandle.create(
                session,
                new OrderHandleImpl(httpClient, requestTimout, session, order, keyPair, accountUrl, orderUrl),
                OrderHandle.class
        );
    }
}
