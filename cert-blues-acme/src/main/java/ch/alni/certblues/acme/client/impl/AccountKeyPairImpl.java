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

import org.slf4j.Logger;

import ch.alni.certblues.acme.client.AccountKeyPair;
import ch.alni.certblues.acme.client.AcmeRequest;
import ch.alni.certblues.acme.client.JwsObject;
import ch.alni.certblues.acme.jws.JwsHeader;
import ch.alni.certblues.acme.jws.KeyVaultEntry;
import ch.alni.certblues.acme.jws.Thumbprints;

import static org.slf4j.LoggerFactory.getLogger;

class AccountKeyPairImpl implements AccountKeyPair {

    private static final Logger LOG = getLogger(AccountKeyPairImpl.class);

    private final KeyVaultEntry keyVaultEntry;
    private final String algorithm;

    AccountKeyPairImpl(KeyVaultEntry keyVaultEntry, String algorithm) {
        this.keyVaultEntry = keyVaultEntry;
        this.algorithm = algorithm;
    }

    private static String toPayload(Object request) {
        if (request instanceof AcmeRequest) {
            return Payloads.serialize(request);
        }
        else if (request instanceof String) {
            return (String) request;
        }
        else {
            throw new IllegalArgumentException("Invalid payload type, it must be either AcmeRequest or String");
        }
    }

    @Override
    public JwsObject sign(String requestUrl, Object request, String nonce) {
        final JwsHeader header = JwsHeader.builder()
                .jwk(keyVaultEntry.getPublicJwk())
                .alg(algorithm)
                .url(requestUrl)
                .nonce(nonce)
                .build();

        final String payload = toPayload(request);
        LOG.info("creating a JSON web signature over '{}'", payload);

        return Jws.createJws(keyVaultEntry, header, payload);
    }

    @Override
    public JwsObject sign(String requestUrl, String keyId, Object request, String nonce) {
        final JwsHeader header = JwsHeader.builder()
                .kid(keyId)
                .alg(algorithm)
                .url(requestUrl)
                .nonce(nonce)
                .build();

        final String payload = toPayload(request);
        LOG.info("creating a JSON web signature over '{}' with kid={}", payload, keyId);

        return Jws.createJws(keyVaultEntry, header, payload);
    }

    @Override
    public String getPublicKeyThumbprint() {
        return Thumbprints.getSha256Thumbprint(keyVaultEntry.getPublicJwk());
    }
}
