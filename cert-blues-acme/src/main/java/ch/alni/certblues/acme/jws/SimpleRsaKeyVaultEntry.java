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

package ch.alni.certblues.acme.jws;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.UUID;

/**
 * A simple key vault entry wrapping an RSA asymmetric key pair.
 */
public class SimpleRsaKeyVaultEntry implements KeyVaultEntry {
    private final String kid = UUID.randomUUID().toString();
    private final KeyPair keyPair;

    public SimpleRsaKeyVaultEntry(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    @Override
    public String sign(String alg, String content) {
        try {
            final Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(keyPair.getPrivate());

            signature.update(content.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public PublicJwk getPublicJwk() {
        final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        final String n = encoder.encodeToString(publicKey.getModulus().toByteArray());
        final String e = encoder.encodeToString(publicKey.getPublicExponent().toByteArray());

        return RsaPublicJwk.builder()
                .kid(kid)
                .use("sig")
                .alg("RS256")
                .n(n)
                .e(e)
                .build();
    }
}
