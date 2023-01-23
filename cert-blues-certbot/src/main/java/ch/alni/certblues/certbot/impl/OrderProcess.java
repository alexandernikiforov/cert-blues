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

import com.google.common.base.Preconditions;

import org.slf4j.Logger;

import java.util.List;

import ch.alni.certblues.acme.protocol.Authorization;
import ch.alni.certblues.acme.protocol.Order;
import ch.alni.certblues.certbot.CertBotException;
import ch.alni.certblues.certbot.CertificateRequest;
import ch.alni.certblues.certbot.events.OrderCheckNeededEvent;
import ch.alni.certblues.certbot.events.OrderCreatedEvent;
import ch.alni.certblues.certbot.events.OrderReadyEvent;
import ch.alni.certblues.certbot.events.OrderStateEvent;
import ch.alni.certblues.certbot.events.OrderStateListener;
import ch.alni.certblues.certbot.events.OrderValidEvent;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The process of ordering a certificate.
 */
public class OrderProcess {
    private static final Logger LOG = getLogger(OrderProcess.class);

    private final Sinks.One<String> subject = Sinks.one();
    private final Mono<String> certificateMono = subject.asMono().cache();

    private final OrderStateListener listener;
    private final CertificateRequest certificateRequest;

    private String orderUrl;

    /**
     * Create a new certificate ordering process.
     */
    OrderProcess(CertificateRequest certificateRequest, OrderStateListener listener) {
        this.listener = listener;
        this.certificateRequest = certificateRequest;
    }

    /**
     * Returns the mono for the certificate to be issued as the result of this process.
     */
    public Mono<String> getCertificate() {
        return certificateMono;
    }

    public CertificateRequest getCertificateRequest() {
        return certificateRequest;
    }

    public synchronized void onOrderCreated(Order order, String orderUrl) {
        this.orderUrl = orderUrl;

        switch (order.status()) {
            case PENDING:
                LOG.info("certificate order created {}, order URL {}", order, orderUrl);
                publish(new OrderCreatedEvent(this, order));
                break;
            case INVALID:
                LOG.info("error while creating the order {}", order);
                subject.tryEmitError(new CertBotException("order creation error: " + order.error()));
                break;
            case PROCESSING:
                LOG.info("waiting for certificate issue with the order {}", order);
                publish(new OrderCheckNeededEvent(this, orderUrl));
                break;
            case READY:
                LOG.info("all authorizations are valid for {}", order);
                publish(new OrderReadyEvent(this, order.finalizeUrl(), orderUrl));
                break;
            case VALID:
                LOG.info("certificate issued and is ready for download for {}", order);
                publish(new OrderValidEvent(this, order.certificate()));
                break;
            default:
                throw new IllegalArgumentException("unknown status " + order.status());
        }
    }

    public synchronized void onOrderProvisioned() {
        Preconditions.checkNotNull(orderUrl, "order URL is not known");

        LOG.info("order provisioned and submitted for URL {}", orderUrl);
        publish(new OrderCheckNeededEvent(this, orderUrl));
    }

    public synchronized void onOrderChanged(Tuple2<Order, List<Authorization>> orderWithAuthorizations) {
        Preconditions.checkNotNull(orderUrl, "order URL is not known");

        final Order order = orderWithAuthorizations.getT1();
        final List<Authorization> authorizations = orderWithAuthorizations.getT2();

        switch (order.status()) {
            case PENDING:
                LOG.info("order is processing {}", order);
                publish(new OrderCheckNeededEvent(this, orderUrl));
                break;
            case INVALID:
                LOG.error("error while processing the order {}\nAuthorizations: {}", order, authorizations);
                subject.tryEmitError(new CertBotException("order processing error: " + order.error()));
                break;
            case PROCESSING:
                LOG.info("waiting for certificate issue with the order {}", order);
                publish(new OrderCheckNeededEvent(this, orderUrl));
                break;
            case READY:
                LOG.info("all authorizations are valid for {}", order);
                publish(new OrderReadyEvent(this, order.finalizeUrl(), orderUrl));
                break;
            case VALID:
                LOG.info("certificate is issued and ready for download for {}", order);
                publish(new OrderValidEvent(this, order.certificate()));
                break;
            default:
                throw new IllegalArgumentException("unknown status " + order.status());
        }
    }

    public synchronized void onCertificateDownloaded(String certificate) {
        Preconditions.checkNotNull(orderUrl, "order URL is not known");

        LOG.info("certificate has been issued for order with URL {}", orderUrl);
        subject.tryEmitValue(certificate);
    }

    void publish(OrderStateEvent event) {
        event.accept(listener);
    }

    synchronized void fail(Throwable throwable) {
        subject.tryEmitError(throwable);
    }
}
