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

package ch.alni.certblues.acme.client.access;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;

import ch.alni.certblues.acme.client.Directory;
import ch.alni.certblues.acme.client.request.NonceRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class NonceAccessorTest {

    private static final String NEW_NONCE_URL = "newNonceUrl";
    private static final String NONCE_1 = "nonce1";
    private static final String NONCE_2 = "nonce2";
    private static final String NONCE_3 = "nonce3";
    private static final Directory DIRECTORY = Directory.builder()
            .newNonce(NEW_NONCE_URL)
            .newAccount("newAccountUrl")
            .newOrder("newOrderUrl")
            .build();

    private final DirectoryAccessor directoryAccessor = Mockito.mock(DirectoryAccessor.class);
    private final NonceRequest nonceRequest = Mockito.mock(NonceRequest.class);
    @BeforeEach
    void setUp() {
        // directory accessor always returns the same directory
        Mockito.when(directoryAccessor.getDirectory()).thenReturn(Mono.just(DIRECTORY));

        // the nonce request brings back the sequence ot nonce values
        Mockito.when(nonceRequest.getNonce(NEW_NONCE_URL))
                .thenReturn(Mono.just(NONCE_1))
                .thenReturn(Mono.just(NONCE_2))
                .thenReturn(Mono.just(NONCE_3));
    }

    @Test
    void getNonceValues() {
        final NonceAccessor nonceAccessor = new NonceAccessor(nonceRequest, directoryAccessor);
        StepVerifier
                .withVirtualTime(nonceAccessor::getNonce)
                .expectNext(NONCE_1)
                .verifyTimeout(Duration.ofMillis(1000L));
    }

    @Test
    void getNonceValuesWithTwoUpdates() {
        final NonceAccessor nonceAccessor = new NonceAccessor(nonceRequest, directoryAccessor);
        nonceAccessor.update();
        nonceAccessor.update();

        StepVerifier
                .withVirtualTime(nonceAccessor::getNonce)
                .expectNext(NONCE_3)
                .verifyTimeout(Duration.ofMillis(1000L));
    }
}
