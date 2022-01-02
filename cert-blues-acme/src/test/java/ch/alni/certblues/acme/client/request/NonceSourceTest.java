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

package ch.alni.certblues.acme.client.request;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class NonceSourceTest {
    private static final String NONCE_1 = "nonce1";
    private static final String NONCE_2 = "nonce2";
    private static final String NONCE_3 = "nonce3";

    @Test
    void shouldQueryNonceIfEmpty() {
        final Mono<String> nonceMono = Mono.just(NONCE_1);
        final NonceSource nonceSource = new NonceSource(nonceMono);

        StepVerifier.withVirtualTime(nonceSource::getNonce)
                .expectNext(NONCE_1)
                .verifyComplete();
    }

    @Test
    void shouldGetNonce() {
        final Mono<String> nonceMono = Mono.just(NONCE_1);
        final NonceSource nonceSource = new NonceSource(nonceMono);

        nonceSource.update(NONCE_2);

        StepVerifier.withVirtualTime(nonceSource::getNonce)
                .expectNext(NONCE_2)
                .verifyComplete();

        // next attempt to get the nonce will return
        StepVerifier.withVirtualTime(nonceSource::getNonce)
                .expectNext(NONCE_1)
                .verifyComplete();

        nonceSource.update(NONCE_3);

        StepVerifier.withVirtualTime(nonceSource::getNonce)
                .expectNext(NONCE_3)
                .verifyComplete();
    }
}
