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

package ch.alni.certblues.azure.config;

import com.azure.security.keyvault.keys.cryptography.models.SignatureAlgorithm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * Properties to configure Azure-based components.
 */
@ConfigurationProperties(prefix = "azure")
@ConstructorBinding
public class AzureConfigurationProperties {

    private final AccountKeyProperties accountKey;

    private final KeyVaultProperties keyVault;

    private final QueueStorageProperties queueStorage;

    public AzureConfigurationProperties(AccountKeyProperties accountKey, KeyVaultProperties keyVault,
                                        QueueStorageProperties queueStorage) {
        this.accountKey = accountKey;
        this.keyVault = keyVault;
        this.queueStorage = queueStorage;
    }

    public AccountKeyProperties getAccountKey() {
        return accountKey;
    }

    public KeyVaultProperties getKeyVault() {
        return keyVault;
    }

    public QueueStorageProperties getQueueStorage() {
        return queueStorage;
    }

    public static class AccountKeyProperties {

        private final String id;
        private final String signatureAlg;

        public AccountKeyProperties(String id, String signatureAlg) {
            this.id = id;
            this.signatureAlg = signatureAlg;
        }

        /**
         * ID of the key in the key vault that represents the account key. This is the full path representing the key
         * resource within the key vault.
         */
        public String getId() {
            return id;
        }

        /**
         * Returns the signature algorithm to be used by the key to sign content. See {@link SignatureAlgorithm} for
         * possible names.
         */
        public String getSignatureAlg() {
            return signatureAlg;
        }
    }

    public static class KeyVaultProperties {
        private final String url;

        public KeyVaultProperties(String url) {
            this.url = url;
        }

        /**
         * URL of the key vault.
         */
        public String getUrl() {
            return url;
        }
    }

    /**
     * Properties for the storage account to hold the certificate requests.
     */
    public static class QueueStorageProperties {
        private final String serviceUrl;
        private final String requestQueueName;

        public QueueStorageProperties(String serviceUrl, String requestQueueName) {
            this.serviceUrl = serviceUrl;
            this.requestQueueName = requestQueueName;
        }

        /**
         * The service URL of the storage account.
         */
        public String getServiceUrl() {
            return serviceUrl;
        }

        /**
         * The name of the queue to pass the certificate requests into.
         */
        public String getRequestQueueName() {
            return requestQueueName;
        }
    }
}
