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

import org.slf4j.Logger;

import ch.alni.certblues.acme.protocol.AcmeServerException;
import reactor.core.Exceptions;
import reactor.util.retry.Retry;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The retry handler that updates the nonce and then retries the operation.
 */
public class RetryHandler {
    private static final Logger LOG = getLogger(RetryHandler.class);

    private final Retry retry;

    public RetryHandler(long numberOfRetries) {
        retry = Retry.from(companion -> companion.handle((retrySignal, sink) -> {
            final Throwable failure = retrySignal.failure();
            final boolean retryable = isRetryable(failure);
            if (retryable && retrySignal.totalRetriesInARow() <= numberOfRetries) {
                LOG.info("Retrying last request to the server");
                sink.next(retrySignal.totalRetriesInARow());
            }
            else if (retryable) {
                sink.error(Exceptions.retryExhausted(
                        retrySignal.totalRetriesInARow() + " attempts failed", retrySignal.failure()
                ));
            }
            else {
                sink.error(retrySignal.failure());
            }
        }));
    }

    private static boolean isRetryable(Throwable throwable) {
        return throwable instanceof AcmeServerException && ((AcmeServerException) throwable).isRetryable();
    }

    public Retry getRetry() {
        return retry;
    }
}
