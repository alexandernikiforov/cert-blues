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

import org.slf4j.Logger;

import ch.alni.certblues.acme.client.Directory;
import ch.alni.certblues.acme.client.request.NonceRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import static org.slf4j.LoggerFactory.getLogger;

public class NonceAccessor {
    private static final Logger LOG = getLogger(NonceAccessor.class);

    private final Flux<String> nonceValues;
    private final Mono<String> requestNonceMono;
    private final Sinks.Many<String> nonceSubject = Sinks.many().multicast().directBestEffort();

    public NonceAccessor(NonceRequest nonceRequest, DirectoryAccessor directoryAccessor) {
        // request to get a single nonce value (it is not null)
        requestNonceMono = directoryAccessor.getDirectory()
                .map(Directory::newNonce)
                .flatMap(nonceRequest::getNonce);

        // combine the streams from the two streams, and share it to all subscribers
        nonceValues = requestNonceMono
                .mergeWith(nonceSubject.asFlux())
                .log()
                .share()
                // store the last emitted value
                .cache(1);
    }

    public Flux<String> getNonceValues() {
        return nonceValues;
    }

    public void update() {
        // get the next nonce and push it to the subject if it is not null
        requestNonceMono.subscribe(
                nonce -> nonceSubject.emitNext(nonce, Sinks.EmitFailureHandler.FAIL_FAST),
                throwable -> LOG.error("unexpected error while getting a new nonce", throwable)
        );
    }
}
