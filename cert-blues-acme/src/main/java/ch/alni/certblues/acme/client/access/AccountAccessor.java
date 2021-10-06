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

import ch.alni.certblues.acme.client.Account;
import ch.alni.certblues.acme.client.AccountResourceRequest;
import ch.alni.certblues.acme.client.Directory;
import ch.alni.certblues.acme.client.request.AccountRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * TODO: javadoc
 */
public class AccountAccessor {

    private final DirectoryAccessor directoryAccessor;
    private final NonceAccessor nonceAccessor;
    private final KeyVaultKeyAccessor keyVaultKeyAccessor;
    private final AccountRequest accountRequest;

    public AccountAccessor(DirectoryAccessor directoryAccessor,
                           NonceAccessor nonceAccessor,
                           KeyVaultKeyAccessor keyVaultKeyAccessor,
                           AccountRequest accountRequest) {
        this.directoryAccessor = directoryAccessor;
        this.nonceAccessor = nonceAccessor;
        this.keyVaultKeyAccessor = keyVaultKeyAccessor;
        this.accountRequest = accountRequest;


    }

    public Mono<Account> getAccount(AccountResourceRequest resourceRequest) {
        Flux.combineLatest(
                nonceAccessor.getNonceValues(),
                directoryAccessor.getDirectory(),
                (nonce, directory) -> keyVaultKeyAccessor.sign(directory.newAccount(), resourceRequest, nonce)
        )

        directoryAccessor.getDirectory()
                // get the account URL
                .map(Directory::newAccount)
                // create the signed request
                .
                .flatMap(accountUrl -> keyVaultKeyAccessor.sign(accountUrl, resourceRequest, ))
        /*
         */
        return null;
    }
}
