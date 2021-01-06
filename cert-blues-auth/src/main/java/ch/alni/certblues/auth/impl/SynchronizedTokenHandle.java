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

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import ch.alni.certblues.auth.TokenHandle;
import ch.alni.certblues.auth.TokenResponse;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Token handle that synchronizes access to the inner future.
 */
class SynchronizedTokenHandle implements TokenHandle {
    private static final Logger LOG = getLogger(SynchronizedTokenHandle.class);

    private final Consumer<CompletableFuture<TokenResponse>> tokenUpdate;
    private CompletableFuture<TokenResponse> future;

    /**
     * Creates a new handle.
     *
     * @param tokenUpdate procedure to get a new token and complete the future with the result
     */
    SynchronizedTokenHandle(Consumer<CompletableFuture<TokenResponse>> tokenUpdate) {
        this.tokenUpdate = tokenUpdate;
    }

    @Override
    public synchronized CompletableFuture<TokenResponse> getToken() {
        if (null == future) {
            LOG.info("getting a new token");

            // start a new request
            future = new CompletableFuture<>();
            tokenUpdate.accept(future);
            return future;
        }
        else if (future.isDone()) {
            if (future.isCompletedExceptionally() || future.isCancelled() || (future.join().isExpired())) {
                LOG.info("starting the token renewal");
                // failed last time or cancelled or expired - start renewal
                future = new CompletableFuture<>();
                tokenUpdate.accept(future);
            }
            LOG.info("returning the current token");
            // else do not yet renew and return what we have if completed successfully
            return future;
        }
        else {
            LOG.info("waiting for the token request completion");
            // not yet complete, return the current one
            return future;
        }
    }

    @Override
    public synchronized CompletableFuture<TokenResponse> renewToken() {
        LOG.info("renewing the current token, or getting a new one if it does not exist");
        if (null == future || future.isDone()) {
            LOG.info("getting a new token");

            // start a new request
            future = new CompletableFuture<>();
            tokenUpdate.accept(future);
        }
        else {
            LOG.info("waiting for the token request completion");
        }

        return future;
    }
}
