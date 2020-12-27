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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import ch.alni.certblues.acme.json.ObjectMapperFactory;

import static org.assertj.core.api.Assertions.assertThat;

class PublicJwkTest {

    @Test
    void testRsaPublicKey() throws IOException {
        final var mapper = ObjectMapperFactory.getObjectMapper();
        final PublicJwk publicJwk = mapper.readerFor(PublicJwk.class).readValue(new InputStreamReader(
                getClass().getResourceAsStream("/rsa-public-key.json"), StandardCharsets.UTF_8
        ));

        String result = mapper.writeValueAsString(publicJwk);

        assertThat(publicJwk).isInstanceOf(RsaPublicJwk.class);
    }

    @Test
    void testEcPublicKey() throws IOException {
        final var mapper = ObjectMapperFactory.getObjectMapper();
        final EcPublicJwk publicJwk = mapper.readerFor(PublicJwk.class).readValue(new InputStreamReader(
                getClass().getResourceAsStream("/ec-public-key.json"), StandardCharsets.UTF_8
        ));

        assertThat(publicJwk).isInstanceOf(EcPublicJwk.class);
    }

}
