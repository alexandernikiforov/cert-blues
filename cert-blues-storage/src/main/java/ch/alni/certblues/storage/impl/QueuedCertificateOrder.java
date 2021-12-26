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

import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.alni.certblues.storage.CertificateOrder;
import ch.alni.certblues.storage.JsonTransform;
import ch.alni.certblues.storage.KeyType;

/**
 * The certificate order that is extracted from the queue.
 */
class QueuedCertificateOrder extends CertificateOrder implements JsonTransform {

    private final CertificateOrder certificateOrder;
    private final String popReceipt;
    private final String messageId;

    QueuedCertificateOrder(CertificateOrder certificateOrder, String popReceipt, String messageId) {
        this.certificateOrder = certificateOrder;
        this.popReceipt = popReceipt;
        this.messageId = messageId;
    }

    @Override
    public int keySize() {
        return certificateOrder.keySize();
    }

    @Override
    public KeyType keyType() {
        return certificateOrder.keyType();
    }

    @Override
    public int validityInMonths() {
        return certificateOrder.validityInMonths();
    }

    @Override
    public ImmutableList<String> dnsNames() {
        return certificateOrder.dnsNames();
    }

    @JsonIgnore
    public String getPopReceipt() {
        return popReceipt;
    }

    @JsonIgnore
    public String getMessageId() {
        return messageId;
    }
}
