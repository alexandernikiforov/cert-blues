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

package ch.alni.certblues.storage;

import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.alni.certblues.storage.queue.MessageId;

/**
 * The certificate order that is extracted from the queue.
 */
public final class QueuedCertificateOrder extends CertificateOrder {

    private final CertificateOrder certificateOrder;
    private final MessageId messageId;

    public QueuedCertificateOrder(CertificateOrder certificateOrder, MessageId messageId) {
        this.certificateOrder = certificateOrder;
        this.messageId = messageId;
    }

    @JsonIgnore
    public MessageId getMessageId() {
        return messageId;
    }

    @Override
    public CertificateRequest certificateRequest() {
        return certificateOrder.certificateRequest();
    }

    @Override
    public String orderUrl() {
        return certificateOrder.orderUrl();
    }

    @Override
    public ImmutableList<String> challengeUrls() {
        return certificateOrder.challengeUrls();
    }
}
