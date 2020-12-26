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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import ch.alni.certblues.acme.json.ObjectMapperFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ThumbprintsTest {

    @Test
    void getSha256Thumbprint() throws Exception {
        final ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
        final PublicJwk publicJwk = objectMapper.readerFor(PublicJwk.class).readValue(
                new InputStreamReader(getClass().getResourceAsStream("/jwk-example.json"), StandardCharsets.UTF_8)
        );

        assertThat(Thumbprints.getSha256Thumbprint(publicJwk)).isEqualTo("NzbLsXh8uDCcd-6MNwXF4W_7noWXFZAfHkxZsRGC9Xs");
    }

    @Test
    void getSha256ThumbprintForPebble() throws Exception {
        final ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
        final PublicJwk publicJwk = objectMapper.readerFor(PublicJwk.class).readValue(
                new InputStreamReader(getClass().getResourceAsStream("/jwk-example-pebble.json"), StandardCharsets.UTF_8)
        );

        assertThat(Thumbprints.getSha256Thumbprint(publicJwk)).isEqualTo("s-aVZBN444ZRQPQyZw66-mDh2nVd_fJ7Ed6T--6Pzyo");
    }

}
