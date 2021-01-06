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

import com.google.auto.value.AutoValue;

import java.time.Duration;

/**
 * Connection options.
 */
@AutoValue
public abstract class ConnectionOptions {

    public static Builder builder() {
        return new AutoValue_ConnectionOptions.Builder();
    }

    /**
     * Returns the timeout to open the connection.
     */
    public abstract Duration getConnectionTimeout();

    /**
     * Returns the timeout to wait for the request to come.
     */
    public abstract Duration getRequestTimeout();

    /**
     * Returns the interval to retry the failed request.
     */
    public abstract Duration getRetryInterval();

    /**
     * Returns the the number of retries for a failed request.
     */
    public abstract int getNumberOfRetries();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setConnectionTimeout(Duration value);

        public abstract Builder setRequestTimeout(Duration value);

        public abstract Builder setRetryInterval(Duration value);

        public abstract Builder setNumberOfRetries(int value);

        public abstract ConnectionOptions build();
    }
}
