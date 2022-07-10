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

package ch.alni.certblues.azure.queue;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.queue.QueueAsyncClient;
import com.azure.storage.queue.QueueServiceAsyncClient;
import com.azure.storage.queue.QueueServiceClientBuilder;

import java.time.Duration;

import ch.alni.certblues.certbot.queue.MessageId;
import ch.alni.certblues.certbot.queue.Queue;
import ch.alni.certblues.certbot.queue.QueuedMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Queue implementation based on the Azure queue storage.
 */
public class AzureQueue implements Queue {
    /**
     * How long the message should be invisible after reading until it appears in the queue for further operations.
     */
    private static final Duration VISIBILITY_TIMEOUT_READ = Duration.ofSeconds(10);

    /**
     * How long the message should be invisible after writing until it appears in the queue for further operations.
     */
    private static final Duration VISIBILITY_TIMEOUT_WRITE = Duration.ofSeconds(10);

    /**
     * How long the message should remain in the queue.
     */
    private static final Duration TIME_TO_LIVE = Duration.ofDays(10L);

    /**
     * How many items to receive from the queue each time.
     */
    private static final int MAX_RECEIVED_QUEUE_ITEMS = 5;

    private final QueueAsyncClient queueClient;

    public AzureQueue(TokenCredential credential, HttpClient httpClient, String queueServiceUrl, String queueName) {
        QueueServiceAsyncClient queueServiceClient = new QueueServiceClientBuilder()
                .credential(credential).httpClient(httpClient).endpoint(queueServiceUrl)
                .buildAsyncClient();

        queueClient = queueServiceClient.getQueueAsyncClient(queueName);
    }

    public AzureQueue(TokenCredential credential, String queueServiceUrl, String queueName) {
        this(credential, null, queueServiceUrl, queueName);
    }

    @Override
    public Flux<QueuedMessage> getMessages() {
        return queueClient.receiveMessages(MAX_RECEIVED_QUEUE_ITEMS, VISIBILITY_TIMEOUT_READ)
                .map(queueMessageItem -> QueuedMessage.create(
                        new MessageIdWithReceipt(queueMessageItem.getMessageId(), queueMessageItem.getPopReceipt()),
                        queueMessageItem.getBody().toString()
                ));
    }

    @Override
    public Mono<MessageId> put(String payload) {
        return queueClient.sendMessageWithResponse(BinaryData.fromString(payload), VISIBILITY_TIMEOUT_WRITE, TIME_TO_LIVE)
                .map(Response::getValue)
                .map(result -> new MessageIdWithReceipt(result.getMessageId(), result.getPopReceipt()));
    }

    @Override
    public Mono<Void> delete(MessageId messageId) {
        if (messageId == null) {
            throw new IllegalArgumentException("messageId cannot be null");
        }

        if (!(messageId instanceof MessageIdWithReceipt)) {
            throw new IllegalArgumentException("cannot restore the message ID for the Azure queue; " +
                    "the provided message ID is of type " + messageId.getClass().getName());
        }

        final MessageIdWithReceipt messageIdWithReceipt = (MessageIdWithReceipt) messageId;
        return queueClient.deleteMessage(messageIdWithReceipt.getMessageId(), messageIdWithReceipt.getPopReceipt());
    }
}
