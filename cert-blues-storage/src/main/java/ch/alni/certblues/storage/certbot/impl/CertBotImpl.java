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

package ch.alni.certblues.storage.certbot.impl;

import org.slf4j.Logger;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.alni.certblues.acme.client.Challenge;
import ch.alni.certblues.acme.client.CreatedResource;
import ch.alni.certblues.acme.client.Order;
import ch.alni.certblues.acme.client.OrderFinalizationRequest;
import ch.alni.certblues.acme.client.OrderStatus;
import ch.alni.certblues.acme.facade.AcmeSession;
import ch.alni.certblues.acme.key.CertificateEntry;
import ch.alni.certblues.storage.certbot.AuthorizationProvisionerFactory;
import ch.alni.certblues.storage.certbot.CertBot;
import ch.alni.certblues.storage.certbot.CertBotException;
import ch.alni.certblues.storage.certbot.CertificateEntryFactory;
import ch.alni.certblues.storage.certbot.CertificateOrder;
import ch.alni.certblues.storage.certbot.CertificateRequest;
import ch.alni.certblues.storage.certbot.CertificateStatus;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static org.slf4j.LoggerFactory.getLogger;

public class CertBotImpl implements CertBot {

    private static final Logger LOG = getLogger(CertBotImpl.class);

    private final Scheduler internal = Schedulers.newSingle("certBot");
    private final Map<CertificateRequest, Sinks.One<CertificateOrder>> requests = new HashMap<>();
    private final Map<CertificateOrder, Sinks.One<CertificateStatus>> orders = new HashMap<>();

    private final AcmeSession session;
    private final CertificateEntryFactory certificateEntryFactory;
    private final AuthorizationProvisionerFactory provisionerFactory;

    public CertBotImpl(AcmeSession session,
                       CertificateEntryFactory certificateEntryFactory,
                       AuthorizationProvisionerFactory provisionerFactory) {
        this.session = session;
        this.certificateEntryFactory = certificateEntryFactory;
        this.provisionerFactory = provisionerFactory;
    }

    @Override
    public synchronized Mono<CertificateOrder> submit(CertificateRequest certificateRequest) {
        LOG.info("submitting a new certificate request {}", certificateRequest);

        final Sinks.One<CertificateOrder> existingSubject = requests.get(certificateRequest);
        if (null != existingSubject) {
            // return existing subject
            return existingSubject.asMono();
        }

        final var orderRequest = OrderRequests.toOrderRequest(certificateRequest);
        final var httpChallengeProvisioner = provisionerFactory.createHttpChallengeProvisioner(certificateRequest);
        final var dnsChallengeProvisioner = provisionerFactory.createDnsChallengeProvisioner(certificateRequest);

        final Mono<CreatedResource<Order>> orderResourceMono = session.createOrder(orderRequest).share();
        final Mono<List<Challenge>> authorizationMono = session.getPublicKeyThumbprint()
                // create a client for provisioning
                .map(thumbprint -> new IdentifierAuthorizationClient(thumbprint, httpChallengeProvisioner, dnsChallengeProvisioner))
                // and then process the authorizations
                .flatMap(provisioner -> orderResourceMono
                        .map(resource -> resource.getResource().authorizations())
                        .flatMap(authorizationUrls -> session.provision(authorizationUrls, provisioner)
                        ));

        // create a subject to propagate the future certificate order
        final Sinks.One<CertificateOrder> subject = Sinks.one();
        requests.putIfAbsent(certificateRequest, subject);

        // schedule the order provisioning
        Mono.zip(orderResourceMono, authorizationMono)
                .publishOn(internal)
                .subscribeOn(internal)
                .subscribe(
                        tuple -> processProvisionedOrder(certificateRequest, tuple.getT1(), tuple.getT2()),
                        throwable -> {
                            LOG.error("certificate order {} cannot be provisioned", certificateRequest);
                            subject.tryEmitError(throwable);
                        }
                );

        return subject.asMono().cache();
    }

    @Override
    public synchronized Mono<CertificateStatus> check(CertificateOrder certificateOrder) {
        LOG.info("checking certificate order {}", certificateOrder);

        final Sinks.One<CertificateStatus> existingSubject = orders.get(certificateOrder);
        if (null != existingSubject) {
            // return existing subject
            return existingSubject.asMono();
        }
        else {
            // create a subject to propagate the certificate status
            final Sinks.One<CertificateStatus> subject = Sinks.one();
            orders.put(certificateOrder, subject);

            session.getOrder(certificateOrder.orderUrl())
                    .publishOn(internal)
                    .subscribeOn(internal)
                    .subscribe(order -> processOrder(certificateOrder, order), subject::tryEmitError);

            return subject.asMono();
        }
    }

    private synchronized void processProvisionedOrder(CertificateRequest certificateRequest,
                                                      CreatedResource<Order> resource, List<Challenge> challenges) {

        final var order = resource.getResource();
        final var subject = requests.get(certificateRequest);

        if (order.status() == OrderStatus.INVALID) {
            LOG.info("error while provisioning the order {}", order);
            subject.tryEmitError(new CertBotException("order provisioning error: " + order.error()));
        }
        else if (order.status() == OrderStatus.PENDING) {
            LOG.info("order provisioned {}, order URL {}, challenges {}",
                    resource.getResource(), resource.getResourceUrl(), challenges);

            // submit the challenges
            final var challengeUrls = challenges.stream().map(Challenge::url).collect(Collectors.toList());
            final var submitChallengeMono = session.submitChallenges(challengeUrls);

            submitChallengeMono
                    .publishOn(internal)
                    .subscribeOn(internal)
                    .subscribe(
                            submittedChallenges -> processSubmittedOrder(certificateRequest, resource, challenges),
                            throwable -> {
                                LOG.error("certificate order {} cannot be submitted", certificateRequest);
                                subject.tryEmitError(throwable);
                            }
                    );
        }
        else {
            processSubmittedOrder(certificateRequest, resource, challenges);
        }
    }

    private synchronized void processSubmittedOrder(CertificateRequest certificateRequest,
                                                    CreatedResource<Order> resource, List<Challenge> challenges) {

        final var order = resource.getResource();
        final var subject = requests.get(certificateRequest);

        if (order.status() == OrderStatus.INVALID) {
            LOG.info("error while submitting the order {}", order);
            subject.tryEmitError(new CertBotException("order submission error: " + order.error()));
        }
        else {
            LOG.info("order submitted {}, order URL {}, challenges {}",
                    resource.getResource(), resource.getResourceUrl(), challenges);

            final var challengeUrls = challenges.stream().map(Challenge::url).collect(Collectors.toList());
            final var certificateOrder = CertificateOrder.builder()
                    .certificateRequest(certificateRequest)
                    .orderUrl(resource.getResourceUrl())
                    .challengeUrls(challengeUrls)
                    .build();

            requests.remove(certificateRequest);
            subject.tryEmitValue(certificateOrder);
            LOG.info("new certificate order has been created {}", certificateOrder);
        }
    }

    private synchronized void processOrder(CertificateOrder certificateOrder, Order order) {
        switch (order.status()) {
            case INVALID:
                final var subject = orders.get(certificateOrder);
                subject.tryEmitError(new CertBotException("order processing error: " + order.error()));
                break;
            case READY:
                LOG.info("order authorization successful for {}", order);
                doProcessReadyOrder(certificateOrder, order);
                break;
            case VALID:
                LOG.info("ordered certificate issued for {}", order);
                doProcessValidOrder(certificateOrder, order);
                break;
            default:
                LOG.info("order is processing {}", order);
                doProcessIncompleteOrder(certificateOrder);
                break;
        }
    }

    private synchronized void doFinishOrder(CertificateOrder certificateOrder) {
        LOG.info("certificate installed for order {}", certificateOrder);
        final var subject = orders.get(certificateOrder);
        orders.remove(certificateOrder);
        subject.tryEmitValue(CertificateStatus.ISSUED);
    }

    private void doProcessValidOrder(CertificateOrder certificateOrder, Order order) {
        final var subject = orders.get(certificateOrder);
        final CertificateEntry certificateEntry = certificateEntryFactory.create(
                certificateOrder.certificateRequest()
        );

        LOG.info("storing certificate for {}", order);
        final Mono<Void> certMono = session.downloadCertificate(order.certificate()).flatMap(certificateEntry::upload);
        certMono
                .publishOn(internal)
                .subscribeOn(internal)
                .subscribe(
                        unused -> LOG.info("certificate uploaded"),
                        throwable -> subject.tryEmitError(new CertBotException("cannot download certificate", throwable)),
                        () -> doFinishOrder(certificateOrder)
                );
    }

    private void doProcessReadyOrder(CertificateOrder certificateOrder, Order order) {
        final var subject = orders.get(certificateOrder);
        final CertificateEntry certificateEntry = certificateEntryFactory.create(
                certificateOrder.certificateRequest()
        );

        LOG.info("starting order finalization for {}", order);
        final var finalizationRequestMono = certificateEntry.createCsr()
                .map(csr -> Base64.getUrlEncoder().withoutPadding().encodeToString(csr))
                .map(encodedCsr -> OrderFinalizationRequest.builder().csr(encodedCsr).build());

        final Mono<Order> orderMono = finalizationRequestMono
                .flatMap(request -> session.finalizeOrder(order.finalizeUrl(), request));

        orderMono
                .publishOn(internal)
                .subscribeOn(internal)
                .subscribe(checkedOrder -> processOrder(certificateOrder, checkedOrder), subject::tryEmitError);
    }

    private void doProcessIncompleteOrder(CertificateOrder certificateOrder) {
        final var subject = orders.get(certificateOrder);

        orders.remove(certificateOrder);
        subject.tryEmitValue(CertificateStatus.PENDING);
    }
}
