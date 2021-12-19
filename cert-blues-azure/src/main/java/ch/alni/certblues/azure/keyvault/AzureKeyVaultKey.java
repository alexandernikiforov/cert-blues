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

package ch.alni.certblues.azure.keyvault;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyAsyncClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.SignatureAlgorithm;
import com.azure.security.keyvault.keys.models.KeyType;
import com.azure.security.keyvault.keys.models.KeyVaultKey;

import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import ch.alni.certblues.acme.key.EcPublicJwk;
import ch.alni.certblues.acme.key.PublicJwk;
import ch.alni.certblues.acme.key.RsaPublicJwk;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.acme.key.Thumbprints;
import reactor.core.publisher.Mono;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the key vault key based on the Azure key from a KeyVault.
 */
public class AzureKeyVaultKey implements SigningKeyPair {
    private static final Logger LOG = getLogger(AzureKeyVaultKey.class);

    private final CryptographyAsyncClient client;
    private final String alg;

    private final Mono<PublicJwk> publicJwkMono;
    private final Mono<String> publicKeyThumbprintMono;

    public AzureKeyVaultKey(TokenCredential credential, String keyId, String alg) {
        this(credential, null, keyId, alg);
    }

    public AzureKeyVaultKey(TokenCredential credential, HttpClient httpClient, String keyId, String alg) {
        this.client = new CryptographyClientBuilder()
                .httpClient(httpClient)
                .keyIdentifier(keyId)
                .credential(credential)
                .buildAsyncClient();

        this.alg = alg;

        // cache the latest result for this key
        publicJwkMono = client.getKey().map(AzureKeyVaultKey::toPublicJwk).cache();
        publicKeyThumbprintMono = publicJwkMono.map(Thumbprints::getSha256Thumbprint).cache();
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

    private static PublicJwk toPublicJwk(KeyVaultKey key) {
        LOG.debug("Converting to public JWK {}", key);

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

    @Override
    public Mono<String> sign(String content) {
        LOG.debug("signing with alg={}", alg);

        final byte[] data = content.getBytes(StandardCharsets.US_ASCII);
        final byte[] digest = createDigest(data);
        final var signatureAlgorithm = SignatureAlgorithm.fromString(alg);

        return client.sign(signatureAlgorithm, digest)
                .map(signResult -> Base64.getUrlEncoder().withoutPadding().encodeToString(signResult.getSignature()));
    }

    @Override
    public Mono<PublicJwk> getPublicJwk() {
        return publicJwkMono;
    }

    @Override
    public String getAlgorithm() {
        return alg;
    }

    @Override
    public Mono<String> getPublicKeyThumbprint() {
        return publicKeyThumbprintMono;
    }
}
