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
import com.azure.security.keyvault.keys.models.KeyVaultKey;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the key vault.
 */
public class KeyVaultTest {

    public static final String KEY_VAULT_URL = "https://cert-blues-dev.vault.azure.net";
    public static final String ACCOUNT_KEY = "accountKey";

    @Test
    public void testKeyVaultOperations() {
        final var credential = new DefaultAzureCredentialBuilder()
                .build();

        // Azure SDK client builders accept the credential as a parameter
        final var client = new KeyClientBuilder()
                .vaultUrl(KEY_VAULT_URL)
                .credential(credential)
                .buildClient();

/*
        final KeyVaultKey key = client.createRsaKey(new CreateRsaKeyOptions(ACCOUNT_KEY)
                .setKeySize(2048)
                .setKeyOperations(
                        KeyOperation.SIGN, KeyOperation.VERIFY, KeyOperation.DECRYPT, KeyOperation.ENCRYPT
                )
                .setExpiresOn(OffsetDateTime.now().plusYears(1))
        );

        assertThat(key).isNotNull();
*/
        final KeyVaultKey existingKey = client.getKey(ACCOUNT_KEY);
        assertThat(existingKey).isNotNull();
    }

}
