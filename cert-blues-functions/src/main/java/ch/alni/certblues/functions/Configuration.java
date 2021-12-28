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

package ch.alni.certblues.functions;

import com.google.auto.value.AutoValue;

/**
 * Configuration for the functions in this project.
 */
@AutoValue
public abstract class Configuration {

    public static Builder builder() {
        return new AutoValue_Configuration.Builder();
    }

    /**
     * URL to the key vault. The key vault holds the generated certificates as well as the account key.
     */
    public abstract String keyVaultUrl();

    /**
     * ID of the account key in the key vault. This is the absolute path as provided for the key vault.
     */
    public abstract String accountKeyId();

    /**
     * Algorithm to create the signature with the account key. For example, RS256.
     */
    public abstract String accountKeySignatureAlg();

    /**
     * URL of the directory on the ACME server. This is the entry point to talk to the ACME server.
     */
    public abstract String directoryUrl();

    /**
     * URL of the queue service used to store the certificate requests and orders.
     */
    public abstract String queueServiceUrl();

    /**
     * Name of the queue to hold the certificate orders.
     */
    public abstract String orderQueueName();

    /**
     * Name of the queue to hold the certificate requests.
     */
    public abstract String requestQueueName();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder keyVaultUrl(String value);

        public abstract Builder accountKeyId(String value);

        public abstract Builder accountKeySignatureAlg(String value);

        public abstract Builder directoryUrl(String value);

        public abstract Builder queueServiceUrl(String value);

        public abstract Builder orderQueueName(String value);

        public abstract Builder requestQueueName(String value);

        public abstract Configuration build();
    }
}
