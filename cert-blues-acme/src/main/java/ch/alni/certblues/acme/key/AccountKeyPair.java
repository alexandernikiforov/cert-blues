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
 * Abstraction over a key entry in the key vault that holds a key pair and is used to create signatures.
 */
public interface AccountKeyPair {

    /**
     * Signs the given content with the provided algorithm and returns the result.
     *
     * @param content the content to be signed.
     * @return the cryptographic signature as base64-urlencoded string (as Mono)
     */
    Mono<String> sign(String content);

    /**
     * Returns the JSON representation of the public key used by this entry (as Mono).
     */
    Mono<PublicJwk> getPublicJwk();

    /**
     * Returns the signature algorithm used by the private key of this pair.
     */
    String getAlgorithm();
}
