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

import com.fasterxml.jackson.core.JsonProcessingException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import ch.alni.certblues.acme.client.JwsObject;
import ch.alni.certblues.acme.json.ObjectMapperFactory;
import ch.alni.certblues.acme.jws.JwsHeader;
import ch.alni.certblues.acme.jws.KeyVaultEntry;

/**
 * Static utility class to create the JSON web signatures.
 */
final class Jws {

    private Jws() {
        // static utility class
    }

    public static JwsObject createJws(KeyVaultEntry keyVaultEntry, JwsHeader header, String payload) {
        // create the signature
        final String protectedHeader = encode(serialize(header));
        final String encodedPayload = encode(payload);

        final String content = protectedHeader + '.' + encodedPayload;
        final String signature = keyVaultEntry.sign(header.alg(), content);

        return JwsObject.builder()
                .protectedHeader(protectedHeader)
                .payload(encodedPayload)
                .signature(signature)
                .build();
    }

    public static String createJwt(KeyVaultEntry keyVaultEntry, JwsHeader header, String payload) {
        final JwsObject jwsObject = createJws(keyVaultEntry, header, payload);

        final String protectedHeader = jwsObject.protectedHeader();
        final String encodedPayload = jwsObject.payload();
        final String signature = jwsObject.signature();

        return protectedHeader + '.' + encodedPayload + '.' + signature;
    }

    private static String encode(String content) {
        final var encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String serialize(Object header) {
        final var objectMapper = ObjectMapperFactory.getObjectMapper();
        try {
            return objectMapper.writeValueAsString(header);
        }
        catch (JsonProcessingException e) {
            throw new IllegalArgumentException("cannot serialize the given header to JSON: " + header, e);
        }
    }
}
