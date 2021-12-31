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

package ch.alni.certblues.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;

import org.slf4j.Logger;

import java.util.stream.Collectors;

import ch.alni.certblues.acme.client.AccountRequest;
import ch.alni.certblues.acme.client.Identifier;
import ch.alni.certblues.acme.client.OrderRequest;
import ch.alni.certblues.acme.facade.AcmeClient;
import ch.alni.certblues.acme.key.CertificateEntry;
import ch.alni.certblues.azure.keyvault.AzureKeyVaultCertificateBuilder;
import ch.alni.certblues.azure.provision.AzureHttpChallengeProvisioner;
import ch.alni.certblues.storage.StorageService;
import ch.alni.certblues.storage.certbot.CertBot;
import ch.alni.certblues.storage.certbot.CertificateRequest;
import ch.alni.certblues.storage.certbot.CertificateStatus;
import ch.alni.certblues.storage.certbot.HttpChallengeProvisioner;
import ch.alni.certblues.storage.certbot.impl.CertBotImpl;
import reactor.core.publisher.Mono;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Function that implements the logic of submitting certificate requests and verifying the certificate orders.
 */
@SuppressWarnings("unused")
public class Function {
    private static final Logger LOG = getLogger(Function.class);

    // static initialization makes sure that context is initialized only once at the start of the instance
    private static final Context CONTEXT = Context.getInstance();

    private static OrderRequest toOrderRequest(CertificateRequest request) {
        return OrderRequest.builder()
                .identifiers(request.dnsNames().stream()
                        .map(dnsName -> Identifier.builder().type("dns").value(dnsName).build())
                        .collect(Collectors.toList()))
                .build();
    }

    private static CertificateEntry toCertificateEntry(CertificateRequest request) {
        return new AzureKeyVaultCertificateBuilder(
                CONTEXT.getCredential(), CONTEXT.getHttpClient(), CONTEXT.getConfiguration().keyVaultUrl()
        ).create(request);
    }

    private static HttpChallengeProvisioner toHttpChallengeProvisioner(CertificateRequest request) {
        return new AzureHttpChallengeProvisioner(CONTEXT.getHttpClient(), request.storageEndpointUrl());
    }

    @FunctionName("submitOrders")
    public void submitOrders(@TimerTrigger(name = "submitOrdersSchedule", schedule = "%Schedule%") String timerInfo,
                             ExecutionContext context) {

        context.getLogger().info("Order submission started: " + timerInfo);

        final StorageService storageService = CONTEXT.getStorageService();
        final AcmeClient acmeClient = CONTEXT.getAcmeClient();

        final var accountRequest = AccountRequest.builder().termsOfServiceAgreed(true).build();
        final var acmeSession = acmeClient.login(CONTEXT.getAccountKeyPair(), accountRequest);

        final CertBot certBot = new CertBotImpl(
                acmeSession, CONTEXT.getCertificateEntryFactory(), CONTEXT.getAuthorizationProvisionerFactory()
        );

        storageService.getCertificateRequests()
                // make the cert bot submit the request
                .concatMap(certBot::submit)
                // at the end first remove the request, then store the order
                .flatMap(certificateOrder -> storageService
                        .remove(certificateOrder.certificateRequest())
                        .then(storageService.store(certificateOrder))
                )
                .onErrorContinue((e, certificateOrder) -> LOG.error("error while processing certificate request", e))
                .subscribe(
                        certificateOrder -> LOG.info("Order submitted {}", certificateOrder),
                        throwable -> LOG.error("cannot submit the order", throwable),
                        () -> LOG.info("order submission completed")
                );

        context.getLogger().info("Order submission completed");
    }

    @FunctionName("checkOrders")
    public void checkOrders(@TimerTrigger(name = "checkOrdersSchedule", schedule = "%Schedule%") String timerInfo,
                            ExecutionContext context) {

        context.getLogger().info("Order verification started: " + timerInfo);

        final StorageService storageService = CONTEXT.getStorageService();
        final AcmeClient acmeClient = CONTEXT.getAcmeClient();

        final var accountRequest = AccountRequest.builder()
                .termsOfServiceAgreed(true).onlyReturnExisting(true)
                .build();
        final var acmeSession = acmeClient.login(CONTEXT.getAccountKeyPair(), accountRequest);

        final CertBot certBot = new CertBotImpl(
                acmeSession, CONTEXT.getCertificateEntryFactory(), CONTEXT.getAuthorizationProvisionerFactory()
        );

        storageService.getCertificateOrders()
                // make the cert bot check the status of the certificate order
                // and select the order where the certificate is issued
                .flatMap(certificateOrder -> certBot
                        .check(certificateOrder)
                        .filter(certificateStatus -> certificateStatus == CertificateStatus.ISSUED)
                        .then(Mono.just(certificateOrder))
                )
                .onErrorContinue((e, certificateOrder) -> LOG.error("error while processing certificate order", e))
                // remove the certificate order
                .flatMap(storageService::remove)
                .subscribe(
                        certificateOrder -> LOG.info("Order completed {}", certificateOrder),
                        throwable -> LOG.error("order verification error", throwable),
                        () -> LOG.info("order verification completed")
                );

        context.getLogger().info("Order verification completed");
    }
}
