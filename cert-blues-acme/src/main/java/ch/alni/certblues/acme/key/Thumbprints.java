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

import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.slf4j.LoggerFactory.getLogger;

public final class Thumbprints {
    private static final Logger LOG = getLogger(Thumbprints.class);

    private Thumbprints() {
    }

    /**
     * Returns the base64-urlencoded SHA-256 thumbprint of the given JWK.
     */
    public static String getSha256Thumbprint(PublicJwk publicJwk) {
        final String value = getKeyAsJson(publicJwk);

        LOG.info("calculating the SHA-256 thumbprint of the public JWK {}", value);
        return getSha256Digest(value);
    }

    public static String getSha256Digest(String value) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            digest.update(value.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest());
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String getKeyAsJson(PublicJwk publicJwk) {
        if (publicJwk instanceof RsaPublicJwk) {
            final var rsaPublicJwk = (RsaPublicJwk) publicJwk;
            return String.format("{\"e\":\"%s\",\"kty\":\"%s\",\"n\":\"%s\"}",
                    rsaPublicJwk.e(), rsaPublicJwk.kty(), rsaPublicJwk.n());
        }
        else if (publicJwk instanceof EcPublicJwk) {
            final var ecPublicJwk = (EcPublicJwk) publicJwk;
            return String.format("{\"crv\":\"%s\",\"kty\":\"%s\",\"x\":\"%s\",\"y\":\"%s\"}",
                    ecPublicJwk.crv(), ecPublicJwk.kty(), ecPublicJwk.x(), ecPublicJwk.y());
        }
        else {
            throw new IllegalArgumentException("unsupported key type: " + publicJwk);
        }
    }
}
