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

/**
 * Properties to configure Azure-based components.
 */
@ConfigurationProperties(prefix = "azure")
public class AzureConfigurationProperties {

    private final AccountKeyProperties accountKey;

    private final KeyVaultProperties certificateKeyVault;

    private final TableStorageProperties tableStorage;

    public AzureConfigurationProperties(AccountKeyProperties accountKey, KeyVaultProperties certificateKeyVault,
                                        TableStorageProperties tableStorage) {
        this.accountKey = accountKey;
        this.certificateKeyVault = certificateKeyVault;
        this.tableStorage = tableStorage;
    }

    public AccountKeyProperties getAccountKey() {
        return accountKey;
    }

    public KeyVaultProperties getCertificateKeyVault() {
        return certificateKeyVault;
    }

    public TableStorageProperties getTableStorage() {
        return tableStorage;
    }

    public record AccountKeyProperties(String id, String signatureAlg) {

        /**
         * ID of the key in the key vault that represents the account key. This is the full path representing the key
         * resource within the key vault.
         */
        @Override
        public String id() {
            return id;
        }

        /**
         * Returns the signature algorithm to be used by the key to sign content. See {@link SignatureAlgorithm} for
         * possible names.
         */
        @Override
        public String signatureAlg() {
            return signatureAlg;
        }
    }

    public record KeyVaultProperties(String url) {

        /**
         * URL of the key vault.
         */
        @Override
        public String url() {
            return url;
        }
    }

    /**
     * Properties for the storage account to hold the certificate requests.
     */
    public record TableStorageProperties(String serviceUrl, String requestTableName) {

        /**
         * The service URL of the storage account.
         */
        @Override
        public String serviceUrl() {
            return serviceUrl;
        }

        /**
         * The name of the table that holds the certificate requests.
         */
        @Override
        public String requestTableName() {
            return requestTableName;
        }
    }
}
