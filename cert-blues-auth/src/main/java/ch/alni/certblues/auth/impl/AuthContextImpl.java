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

package ch.alni.certblues.auth.impl;

import org.slf4j.Logger;

import java.util.concurrent.ScheduledExecutorService;

import ch.alni.certblues.auth.AuthContext;
import ch.alni.certblues.auth.ClientCredentials;
import ch.alni.certblues.auth.ConnectionOptions;
import ch.alni.certblues.auth.ManagedIdentity;
import ch.alni.certblues.auth.TokenHandle;

import static org.slf4j.LoggerFactory.getLogger;

class AuthContextImpl implements AuthContext {

    private static final Logger LOG = getLogger(AuthContextImpl.class);

    private final RequestExecutor requestExecutor;
    private final TokenEndpointClient endpointClient;

    AuthContextImpl(ScheduledExecutorService executorService, ConnectionOptions connectionOptions) {
        this.requestExecutor = new RequestExecutor(executorService, connectionOptions);
        this.endpointClient = new TokenEndpointClient(connectionOptions);
    }

    @Override
    public TokenHandle forClientCredentials(ClientCredentials clientCredentials) {
        LOG.info("creating a new token request for client credentials {}", clientCredentials);

        return new SynchronizedTokenHandle(
                future -> requestExecutor.requestToken(future, () -> endpointClient.getToken(clientCredentials))
        );
    }

    @Override
    public TokenHandle forManagedIdentity(ManagedIdentity managedIdentity) {
        LOG.info("creating a new token request for managed identity {}", managedIdentity);

        return new SynchronizedTokenHandle(
                future -> requestExecutor.requestToken(future, () -> endpointClient.getToken(managedIdentity))
        );
    }
}
