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

package ch.alni.certblues.certbot.impl;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import ch.alni.certblues.certbot.CertificateRequest;
import ch.alni.certblues.certbot.StorageService;
import ch.alni.certblues.certbot.queue.MessageId;
import ch.alni.certblues.certbot.queue.Queue;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import static org.slf4j.LoggerFactory.getLogger;

public class StorageServiceImpl implements StorageService {
    private static final Logger LOG = getLogger(StorageServiceImpl.class);

    // the maps will keep growing, but this is not very bad as not so many requests are expected
    // in th worst case a clean-up thread can be implemented in a later step
    private final Map<CertificateRequest, MessageId> certificateRequests = new HashMap<>();

    /**
     * Queue for the certificate requests.
     */
    private final Queue requests;

    public StorageServiceImpl(Queue requests) {
        this.requests = requests;
    }

    @Override
    public Mono<Void> remove(CertificateRequest certificateRequest) {
        final var messageId = certificateRequests.get(certificateRequest);
        return null == messageId ? Mono.empty() : requests.delete(messageId)
                .doOnSuccess(id -> LOG.info("message with {} successfully removed from request queue", id));
    }

    @Override
    public Flux<CertificateRequest> getCertificateRequests() {
        return requests.getMessages()
                .doOnNext(message -> LOG.info("processing payload {}", message.payload()))
                .map(message -> Tuples.of(message.messageId(), CertificateRequest.of(message.payload())))
                .doOnNext(tuple -> certificateRequests.put(tuple.getT2(), tuple.getT1()))
                .map(Tuple2::getT2);
    }
}
