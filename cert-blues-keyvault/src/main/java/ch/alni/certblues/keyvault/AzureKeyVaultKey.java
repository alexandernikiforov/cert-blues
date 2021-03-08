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

import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.models.SignResult;
import com.azure.security.keyvault.keys.cryptography.models.SignatureAlgorithm;
import com.azure.security.keyvault.keys.models.KeyType;

import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import ch.alni.certblues.acme.key.EcPublicJwk;
import ch.alni.certblues.acme.key.KeyVaultKey;
import ch.alni.certblues.acme.key.PublicJwk;
import ch.alni.certblues.acme.key.RsaPublicJwk;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the key vault key based on the Azure key from a KeyVault.
 */
public class AzureKeyVaultKey implements KeyVaultKey {
    private static final Logger LOG = getLogger(AzureKeyVaultKey.class);

    private final CryptographyClient client;

    public AzureKeyVaultKey(CryptographyClient client) {
        this.client = client;
    }

    private static byte[] createDigest(byte[] data) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data);
            return md.digest();
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("cannot create signature digest", e);
        }
    }

    @Override
    public String sign(String alg, String content) {
        LOG.info("signing with alg={}", alg);

        final byte[] data = content.getBytes(StandardCharsets.US_ASCII);
        final byte[] digest = createDigest(data);
        final var signatureAlgorithm = SignatureAlgorithm.fromString(alg);

        final SignResult result = client.sign(signatureAlgorithm, digest);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(result.getSignature());
    }

    @Override
    public PublicJwk getPublicJwk() {
        final var key = client.getKey();
        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        final KeyType keyType = key.getKeyType();

        final String kid = key.getKey().getId();
        final String kty = key.getKey().getKeyType().toString();

        if (keyType.equals(KeyType.RSA)) {
            final String n = encoder.encodeToString(key.getKey().getN());
            final String e = encoder.encodeToString(key.getKey().getE());

            return RsaPublicJwk.builder()
                    .kid(kid)
                    .kty(kty)
                    .n(n)
                    .e(e)
                    .build();
        }
        else if (keyType.equals(KeyType.EC)) {
            final String x = encoder.encodeToString(key.getKey().getX());
            final String y = encoder.encodeToString(key.getKey().getY());

            return EcPublicJwk.builder()
                    .kid(kid)
                    .kty(kty)
                    .x(x)
                    .y(y)
                    .crv(key.getKey().getCurveName().toString())
                    .build();
        }
        else {
            throw new IllegalArgumentException("unsupported key type: " + keyType);
        }
    }
}
