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

package ch.alni.certblues.storage.impl;

import ch.alni.certblues.storage.CertificateOrder;
import ch.alni.certblues.storage.CertificateRequest;
import ch.alni.certblues.storage.QueuedCertificateOrder;
import ch.alni.certblues.storage.QueuedCertificateRequest;
import ch.alni.certblues.storage.StorageService;
import ch.alni.certblues.storage.queue.Queue;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StorageServiceImpl implements StorageService {

    /**
     * Queue for the certificate requests.
     */
    private final Queue requests;

    /**
     * Queue for the certificate requests.
     */
    private final Queue orders;

    public StorageServiceImpl(Queue requests, Queue orders) {
        this.requests = requests;
        this.orders = orders;
    }

    @Override
    public Mono<Void> store(CertificateRequest certificateRequest) {
        return requests.put(certificateRequest.toJson()).then();
    }

    @Override
    public Mono<Void> remove(QueuedCertificateRequest certificateRequest) {
        final var messageId = certificateRequest.getMessageId();
        return requests.delete(messageId);
    }

    @Override
    public Flux<QueuedCertificateRequest> getCertificateRequests() {
        return requests.getMessages().map(message -> new QueuedCertificateRequest(
                CertificateRequest.of(message.payload()), message.messageId()
        ));
    }

    @Override
    public Flux<QueuedCertificateOrder> getCertificateOrders() {
        return orders.getMessages().map(message -> new QueuedCertificateOrder(
                CertificateOrder.of(message.payload()), message.messageId()
        ));
    }

    @Override
    public Mono<Void> store(CertificateOrder certificateOrder) {
        return requests.put(certificateOrder.toJson()).then();
    }

    @Override
    public Mono<Void> remove(QueuedCertificateOrder certificateOrder) {
        final var messageId = certificateOrder.getMessageId();
        return orders.delete(messageId);
    }
}
