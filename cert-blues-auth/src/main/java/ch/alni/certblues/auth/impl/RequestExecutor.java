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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ch.alni.certblues.auth.AuthContextException;
import ch.alni.certblues.auth.RetryStrategy;
import ch.alni.certblues.auth.TokenEndpointException;
import ch.alni.certblues.auth.TokenResponse;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implements the logic to execute requests to get tokens.
 */
class RequestExecutor {
    private static final Logger LOG = getLogger(RequestExecutor.class);

    private final ScheduledExecutorService executorService;
    private final RetryStrategy retryStrategy;

    RequestExecutor(ScheduledExecutorService executorService, RetryStrategy retryStrategy) {
        this.executorService = executorService;
        this.retryStrategy = retryStrategy;
    }

    /**
     * Runs request to get a new token.
     *
     * @param future  the future to be updated with the token once it is acquired
     * @param request request to be run to acquire the token
     */
    void requestToken(CompletableFuture<TokenResponse> future, Callable<TokenResponse> request) {
        executorService.execute(() -> doRequestToken(future, request));
    }

    private void doRequestToken(CompletableFuture<TokenResponse> future, Callable<TokenResponse> request) {
        LOG.info("executing request");

        try {
            final TokenResponse response = request.call();
            LOG.info("a new token returned {}", response);
            future.complete(response);
        }
        catch (TokenEndpointException e) {
            LOG.error("token endpoint returned an error while getting a new token", e);
            if (e.isRetryable() && retryStrategy.hasNextAttempt()) {
                LOG.debug("token request will be retried");
                final var nextAttemptDelay = retryStrategy.getNextAttemptDelay();
                executorService.schedule(() -> doRequestToken(future, request),
                        nextAttemptDelay.toMillis(), TimeUnit.MILLISECONDS
                );
            }
            else {
                future.completeExceptionally(e);
            }
        }
        catch (AuthContextException e) {
            LOG.info("error while connecting to the token endpoint", e);
            future.completeExceptionally(e);
        }
        catch (Exception e) {
            future.completeExceptionally(new AuthContextException("unknown error while getting a token", e));
        }
    }
}
