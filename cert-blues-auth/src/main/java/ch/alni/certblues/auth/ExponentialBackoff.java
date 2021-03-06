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

package ch.alni.certblues.auth;

import java.time.Duration;

/**
 * Exponential backoff. This class is not thread-safe.
 */
public class ExponentialBackoff implements RetryStrategy {

    private final int retryCount;

    private final Duration minBackoff;

    private final Duration maxBackoff;

    private final Duration deltaBackoff;

    private final boolean fastFirstRetry;

    /**
     * how many times it has been retried
     */
    private int currentRetryNumber = 0;

    private Duration nextAttemptDelay;

    private ExponentialBackoff(Builder builder) {
        this.retryCount = builder.retryCount;
        this.minBackoff = builder.minBackoff;
        this.maxBackoff = builder.maxBackoff;
        this.deltaBackoff = builder.deltaBackoff;
        this.fastFirstRetry = builder.fastFirstRetry;

        update();
    }

    @Override
    public Duration getNextAttemptDelay() {
        if (!hasNextAttempt()) {
            throw new IllegalStateException("this backoff does not provide the next attempt");
        }

        currentRetryNumber += 1;
        final Duration result = this.nextAttemptDelay;
        update(); // calculate next before returning the result
        return result;
    }

    @Override
    public boolean hasNextAttempt() {
        return currentRetryNumber < retryCount && nextAttemptDelay.compareTo(maxBackoff) <= 0;
    }

    private void update() {
        if (currentRetryNumber == 0) {
            // not yet retried
            nextAttemptDelay = minBackoff;
        }
        else if (currentRetryNumber == 1 && fastFirstRetry) {
            // if the fast first retry is configured
            nextAttemptDelay = minBackoff;
        }
        else {
            nextAttemptDelay = nextAttemptDelay.multipliedBy(2).plus(deltaBackoff);
        }
    }

    public static class Builder {

        private int retryCount = 2;

        private Duration minBackoff = Duration.ZERO;

        private Duration maxBackoff = Duration.ofSeconds(60);

        private Duration deltaBackoff = Duration.ofSeconds(2);

        private boolean fastFirstRetry = false;

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder minBackoff(Duration retryCount) {
            this.minBackoff = retryCount;
            return this;
        }

        public Builder maxBackoff(Duration maxBackoff) {
            this.maxBackoff = maxBackoff;
            return this;
        }

        public Builder deltaBackoff(Duration deltaBackoff) {
            this.deltaBackoff = deltaBackoff;
            return this;
        }

        public Builder fastFirstRetry(boolean fastFirstRetry) {
            this.fastFirstRetry = fastFirstRetry;
            return this;
        }

        public ExponentialBackoff build() {
            return new ExponentialBackoff(this);
        }
    }
}
