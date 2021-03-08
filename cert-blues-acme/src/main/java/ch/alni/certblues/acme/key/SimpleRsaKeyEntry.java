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

package ch.alni.certblues.acme.key;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * A simple key vault entry wrapping an RSA asymmetric key pair.
 */
public class SimpleRsaKeyEntry implements KeyVaultKey {
    private final String kid = UUID.randomUUID().toString();
    private final KeyPair keyPair;

    public SimpleRsaKeyEntry(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    private static byte[] getModulus(RSAPublicKey publicKey) {
        final byte[] byteArray = publicKey.getModulus().toByteArray();
        final int byteLength = publicKey.getModulus().bitLength() / 8;
        if (byteArray.length > byteLength) {
            // a nasty fix
            /*
             Note that implementers have found that some cryptographic libraries
             prefix an extra zero-valued octet to the modulus representations they
             return, for instance, returning 257 octets for a 2048-bit key, rather
             than 256.  Implementations using such libraries will need to take
             care to omit the extra octet from the base64url-encoded
             representation.
             */
            return Arrays.copyOfRange(byteArray, byteArray.length - byteLength, byteArray.length);
        }
        else {
            return byteArray;
        }
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
        final String n = encoder.encodeToString(getModulus(publicKey));
        final String e = encoder.encodeToString(publicKey.getPublicExponent().toByteArray());

        return RsaPublicJwk.builder()
                .kid(kid)
                .use("sig")
                .n(n)
                .e(e)
                .build();
    }
}
