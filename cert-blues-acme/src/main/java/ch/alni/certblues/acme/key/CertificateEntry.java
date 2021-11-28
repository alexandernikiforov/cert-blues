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

import reactor.core.publisher.Mono;

/**
 * Abstraction over a certificate entry in the key vault.
 */
public interface CertificateEntry {

    /**
     * Returns the reference to the signing key pair.
     */
    Mono<SigningKeyPair> getSigningKeyPair();

    /**
     * Returns the certificate sign request for this certificate in base64-urlencoded form. This call will start
     * creation of a new certificate or returns a CSR for the already running operation.
     */
    Mono<byte[]> createCsr();

    /**
     * Uploads certificate in PEM format to this entry.
     *
     * @param certificate certificate in PEM format
     */
    Mono<Void> upload(String certificate);
}
