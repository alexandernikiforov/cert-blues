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

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.storage.queue.QueueAsyncClient;
import com.azure.storage.queue.QueueServiceAsyncClient;
import com.azure.storage.queue.QueueServiceClientBuilder;

import java.time.Duration;
import java.util.List;

import ch.alni.certblues.storage.CertificateOrder;
import ch.alni.certblues.storage.KeyType;
import ch.alni.certblues.storage.ProvisionedCertificateOrder;
import ch.alni.certblues.storage.StorageService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StorageServiceImpl implements StorageService {
    /**
     * the queue containing the order requests
     */
    private final static String QUEUE_REQUESTS = "requests";

    /**
     * the queue containing the orders
     */
    private final static String QUEUE_ORDERS = "orders";
    /**
     * How long the message should be invisible after reading or writing until it appears in the queue for further
     * operations.
     */
    private static final Duration VISIBILITY_TIMEOUT = Duration.ofMinutes(15);
    /**
     * How long the message should remain in the queue.
     */
    private static final Duration TIME_TO_LIVE = Duration.ofDays(10L);
    /**
     * How many items to receive from the queue each time.
     */
    private final int MAX_RECEIVED_QUEUE_ITEMS = 5;
    private final QueueAsyncClient requestQueueClient;
    private final QueueAsyncClient orderQueueClient;

    public StorageServiceImpl(TokenCredential credential, HttpClient httpClient, String queueServiceUrl) {
        QueueServiceAsyncClient queueServiceClient = new QueueServiceClientBuilder()
                .credential(credential).httpClient(httpClient).endpoint(queueServiceUrl)
                .buildAsyncClient();

        requestQueueClient = queueServiceClient.getQueueAsyncClient(QUEUE_REQUESTS);
        orderQueueClient = queueServiceClient.getQueueAsyncClient(QUEUE_ORDERS);
    }

    public StorageServiceImpl(TokenCredential credential, String queueServiceUrl) {
        this(credential, null, queueServiceUrl);
    }

    @Override
    public Flux<CertificateOrder> getPendingOrders() {
        return requestQueueClient.receiveMessages(MAX_RECEIVED_QUEUE_ITEMS, VISIBILITY_TIMEOUT)
                .map(queueMessageItem -> new QueuedCertificateOrder(
//                        CertificateOrder.of(queueMessageItem.getBody().toString()),
                        CertificateOrder.builder()
                                .validityInMonths(12)
                                .dnsNames(List.of("testserver.com"))
                                .keyType(KeyType.RSA).keySize(2048)
                                .build(),
                        queueMessageItem.getPopReceipt(),
                        queueMessageItem.getMessageId()
                ));
    }

    @Override
    public Mono<Void> submit(ProvisionedCertificateOrder provisionedCertificateOrder) {
        final var order = provisionedCertificateOrder.certificateOrder();
        if (!(order instanceof QueuedCertificateOrder)) {
            throw new IllegalArgumentException("cannot restore the queue info for the order; " +
                    "the order object is of type " + order.getClass().getName());
        }

        final var queuedCertificateOrder = (QueuedCertificateOrder) order;

        // delete the original order from the request queue and put it into the order queue
        final var deleteMessageMono = requestQueueClient.deleteMessage(
                queuedCertificateOrder.getMessageId(), queuedCertificateOrder.getPopReceipt()
        );
        final var sendMessageMono = orderQueueClient.sendMessageWithResponse(
                provisionedCertificateOrder.toJson(), VISIBILITY_TIMEOUT, TIME_TO_LIVE
        );
        return deleteMessageMono.then(sendMessageMono.then());
    }
}
