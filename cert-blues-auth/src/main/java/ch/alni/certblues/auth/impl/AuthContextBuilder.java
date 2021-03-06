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

import com.google.common.base.Preconditions;

import java.net.http.HttpClient;
import java.util.concurrent.ScheduledExecutorService;

import ch.alni.certblues.auth.AuthContext;
import ch.alni.certblues.auth.ConnectionOptions;
import ch.alni.certblues.auth.RetryStrategy;

public class AuthContextBuilder {

    private ScheduledExecutorService executorService;

    private HttpClient httpClient;

    private ConnectionOptions connectionOptions;

    private RetryStrategy retryStrategy;

    public AuthContextBuilder() {
    }

    public AuthContextBuilder setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    public AuthContextBuilder setConnectionOptions(ConnectionOptions connectionOptions) {
        this.connectionOptions = connectionOptions;
        return this;
    }

    public AuthContextBuilder setRetryStrategy(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
        return this;
    }

    public AuthContextBuilder setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    public AuthContext build() {
        Preconditions.checkArgument(executorService != null, "executor service cannot be null");
        Preconditions.checkArgument(connectionOptions != null, "connection options cannot be null");
        Preconditions.checkArgument(retryStrategy != null, "retry strategy cannot be null");
        Preconditions.checkArgument(httpClient != null, "HTTP client cannot be null");

        return new AuthContextImpl(retryStrategy, executorService, connectionOptions, httpClient);
    }

}
