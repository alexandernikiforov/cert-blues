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

import ch.alni.certblues.acme.client.Account;
import ch.alni.certblues.acme.client.AccountReactive;
import ch.alni.certblues.acme.client.AccountResourceRequest;
import ch.alni.certblues.acme.client.access.KeyVaultKeyAccessor;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import static org.slf4j.LoggerFactory.getLogger;

public class AccountReactiveImpl implements AccountReactive {
    private static final Logger LOG = getLogger(AccountReactiveImpl.class);

    private final KeyVaultKeyAccessor signingKeyPair;
    private final HttpClient httpClient;

    public AccountReactiveImpl(KeyVaultKeyAccessor signingKeyPair, HttpClient httpClient) {
        this.signingKeyPair = signingKeyPair;
        this.httpClient = httpClient;
    }

    @Override
    public Mono<Account> getAccount(String accountUrl, AccountResourceRequest accountResourceRequest) {
        LOG.info("getting account for {} with request {}", accountUrl, accountResourceRequest);

        return null;
/*
        signingKeyPair.sign(accountUrl, accountRequest,)
        return httpClient
                .headers(headers -> headers.add(HttpHeaderNames.CONTENT_TYPE, "application/jose+json"))
                .post()
                .uri(URI.create(accountUrl))
                .responseSingle((response, bufMono) -> bufMono
                        .asString(StandardCharsets.UTF_8)
                        .zipWith(Mono.just(response)))
                .map(responseTuple2 -> toDirectory(responseTuple2.getT1(), responseTuple2.getT2()));
*/
    }
}
