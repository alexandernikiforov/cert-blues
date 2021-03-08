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

package ch.alni.certblues.keyvault;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.KeyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.models.KeyVaultKey;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import ch.alni.certblues.acme.client.AccountRequest;
import ch.alni.certblues.acme.client.AcmeClient;
import ch.alni.certblues.acme.client.Directory;
import ch.alni.certblues.acme.client.DirectoryHandle;
import ch.alni.certblues.acme.client.impl.AcmeClientBuilder;
import ch.alni.certblues.acme.client.impl.KeyPairBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class AcmeClientTest {
    private static final Logger LOG = getLogger(AcmeClientTest.class);

    private static final String DIRECTORY_URL = "https://acme-staging-v02.api.letsencrypt.org/directory";
    private static final String KEY_VAULT_URL = "https://cert-blues-dev.vault.azure.net";
    private static final String ACCOUNT_KEY = "accountKey";

    private final AcmeClient client = new AcmeClientBuilder()
            .build();

    @Test
    void getAccount() {
        final DirectoryHandle directoryHandle = client.getDirectory(DIRECTORY_URL);

        final Directory directory = directoryHandle.getDirectory();

        LOG.debug("directory: {}", directory);

        // initialize the Azure client
        final var credential = new DefaultAzureCredentialBuilder()
                .build();

        // Azure SDK client builders accept the credential as a parameter
        final var client = new KeyClientBuilder()
                .vaultUrl(KEY_VAULT_URL)
                .credential(credential)
                .buildClient();

        final KeyVaultKey existingKey = client.getKey(ACCOUNT_KEY);

        final CryptographyClient cryptographyClient = new CryptographyClientBuilder()
                .credential(credential)
                .keyIdentifier(existingKey.getId())
                .buildClient();

        final var accountKeyPair = new KeyPairBuilder()
                .setAlgorithm("RS256")
                .setKeyVaultKey(new AzureKeyVaultKey(cryptographyClient))
                .build();

        final var accountHandle = directoryHandle.getAccount(accountKeyPair, AccountRequest.builder()
                .termsOfServiceAgreed(true)
                .build());

        assertThat(accountHandle.getAccount()).isNotNull();
    }
}
