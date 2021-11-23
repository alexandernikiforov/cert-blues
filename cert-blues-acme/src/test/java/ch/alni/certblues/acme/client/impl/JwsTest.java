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

package ch.alni.certblues.acme.client.impl;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import ch.alni.certblues.acme.jws.Jws;
import ch.alni.certblues.acme.jws.JwsHeader;
import ch.alni.certblues.acme.jws.JwsObject;
import ch.alni.certblues.acme.key.KeyVaultKey;
import ch.alni.certblues.acme.key.rsa.SimpleRsaKeyEntry;
import io.jsonwebtoken.Jwts;

import static org.assertj.core.api.Assertions.assertThat;

class JwsTest {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(JwsTest.class);

    @Test
    void create() throws Exception {

        // create the RSA-2048 key pair
        final var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);

        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        final KeyVaultKey keyVaultKey = new SimpleRsaKeyEntry(keyPair);

        final String accountRequest = "hello, world!";

        final JwsHeader header = JwsHeader.builder()
                .alg("RS256")
                .nonce("nonce")
                .url("url")
                .kid("kid")
                .build();

        final JwsObject jwsObject = Jws.createJws(keyVaultKey, header, accountRequest);
        assertThat(jwsObject).isNotNull();

        // verify with the JWS library
        Jwts.parserBuilder()
                .setSigningKey(keyPair.getPublic())
                .build()
                .parse(Jws.createJwt(keyVaultKey, header, accountRequest));
    }
}
