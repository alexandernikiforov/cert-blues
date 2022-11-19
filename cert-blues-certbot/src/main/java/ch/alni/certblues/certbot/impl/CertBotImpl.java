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

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.alni.certblues.acme.client.request.CreatedResource;
import ch.alni.certblues.acme.facade.AcmeSession;
import ch.alni.certblues.acme.facade.AuthorizationProvisioningStrategy;
import ch.alni.certblues.acme.protocol.Challenge;
import ch.alni.certblues.acme.protocol.Order;
import ch.alni.certblues.acme.protocol.OrderFinalizationRequest;
import ch.alni.certblues.acme.protocol.OrderRequest;
import ch.alni.certblues.api.CertificateRequest;
import ch.alni.certblues.certbot.AuthorizationProvisionerFactory;
import ch.alni.certblues.certbot.CertBot;
import ch.alni.certblues.certbot.CertificateStore;
import ch.alni.certblues.certbot.events.OrderCheckNeededEvent;
import ch.alni.certblues.certbot.events.OrderCreatedEvent;
import ch.alni.certblues.certbot.events.OrderReadyEvent;
import ch.alni.certblues.certbot.events.OrderStateListener;
import ch.alni.certblues.certbot.events.OrderValidEvent;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static org.slf4j.LoggerFactory.getLogger;

class CertBotImpl implements CertBot {

    private static final Logger LOG = getLogger(CertBotImpl.class);

    // a workaround around warnings in IntelliJ
    private final Scheduler internal = Schedulers.immediate();

    private final Map<CertificateRequest, OrderProcess> requests = new ConcurrentHashMap<>();

    private final AcmeSession session;
    private final CertificateStore certificateStore;
    private final AuthorizationProvisionerFactory provisionerFactory;

    private final OrderStateListener listener = new OrderStateListener() {

        @Override
        public void on(OrderCreatedEvent event) {
            final var process = event.getProcess();
            final var certificateRequest = process.getCertificateRequest();
            final var order = event.getOrder();

            final var httpChallengeProvisioner = provisionerFactory.createHttpChallengeProvisioner(certificateRequest);
            final var dnsChallengeProvisioner = provisionerFactory.createDnsChallengeProvisioner(certificateRequest);
            final var strategy = AuthorizationProvisioningStrategy.of(
                    httpChallengeProvisioner, dnsChallengeProvisioner
            );

            final Mono<List<Challenge>> authorizationMono = session.provision(order.authorizations(), strategy)
                    // submit the returned challenges
                    .flatMap(session::submitChallenges);

            authorizationMono
                    .subscribeOn(internal)
                    .subscribe(
                            challenges -> process.onOrderProvisioned(),
                            throwable -> {
                                LOG.error("certificate order {} cannot be provisioned", certificateRequest);
                                process.fail(throwable);
                            }
                    );
        }

        @Override
        public void on(OrderCheckNeededEvent event) {
            final var orderUrl = event.getOrderUrl();
            final var process = event.getProcess();

            Mono.just(orderUrl)
                    .delayElement(Duration.ofSeconds(2L))
                    .flatMap(session::getOrder)
                    .subscribeOn(internal)
                    .subscribe(process::onOrderChanged,
                            throwable -> {
                                LOG.error("error while checking order status", throwable);
                                process.fail(throwable);
                            });
        }

        @Override
        public void on(OrderReadyEvent event) {
            final var process = event.getProcess();
            final var finalizeUrl = event.getFinalizeUrl();
            final var certificateRequest = process.getCertificateRequest();

            final var finalizationRequestMono = certificateStore.createCsr(certificateRequest)
                    .map(csr -> Base64.getUrlEncoder().withoutPadding().encodeToString(csr))
                    .map(encodedCsr -> OrderFinalizationRequest.builder().csr(encodedCsr).build());

            final Mono<Order> orderMono = finalizationRequestMono
                    .flatMap(request -> session.finalizeOrder(finalizeUrl, request));

            orderMono
                    .subscribeOn(internal)
                    .subscribe(process::onOrderChanged,
                            throwable -> {
                                LOG.error("error while finalizing order", throwable);
                                process.fail(throwable);
                            });
        }

        @Override
        public void on(OrderValidEvent event) {
            final var certificateUrl = event.getCertificateUrl();
            final var process = event.getProcess();
            final var certificateRequest = process.getCertificateRequest();

            // download the certificate and upload it to the certificate store
            final Mono<String> certMono = session.downloadCertificate(certificateUrl)
                    .flatMap(s -> certificateStore
                            .upload(certificateRequest.certificateName(), s)
                            // return the downloaded certificate
                            .then(Mono.just(s)));

            certMono
                    .subscribeOn(internal)
                    .subscribe(
                            process::onCertificateDownloaded,
                            throwable -> {
                                LOG.error("certificate download error", throwable);
                                process.fail(throwable);
                            });
        }

    };

    CertBotImpl(AcmeSession session, CertificateStore certificateStore,
                AuthorizationProvisionerFactory provisionerFactory) {
        this.session = session;
        this.certificateStore = certificateStore;
        this.provisionerFactory = provisionerFactory;
    }

    @Override
    public Mono<String> submit(CertificateRequest certificateRequest) {
        LOG.info("submitting a new certificate request {}", certificateRequest);

        final OrderProcess orderProcess = requests.computeIfAbsent(certificateRequest, this::create);
        return orderProcess.getCertificate();
    }

    private OrderProcess create(CertificateRequest certificateRequest) {
        final OrderProcess orderProcess = new OrderProcess(certificateRequest, listener);

        final OrderRequest orderRequest = OrderRequests.toOrderRequest(certificateRequest);
        final Mono<CreatedResource<Order>> orderResourceMono = session.createOrder(orderRequest);

        orderResourceMono
                .subscribeOn(internal)
                .subscribe(
                        order -> orderProcess.onOrderCreated(order.getResource(), order.getResourceUrl()),
                        throwable -> {
                            LOG.error("certificate order {} cannot be created", certificateRequest);
                            orderProcess.fail(throwable);
                        }
                );

        return orderProcess;
    }
}
