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

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;

import java.util.logging.Level;
import java.util.stream.Collectors;

import ch.alni.certblues.acme.client.Challenge;
import ch.alni.certblues.acme.client.Identifier;
import ch.alni.certblues.acme.client.OrderRequest;
import ch.alni.certblues.acme.client.OrderStatus;
import ch.alni.certblues.acme.client.access.HttpChallengeProvisioner;
import ch.alni.certblues.acme.facade.AcmeSessionFacade;
import ch.alni.certblues.acme.key.CertificateEntry;
import ch.alni.certblues.azure.keyvault.AzureKeyVaultCertificateBuilder;
import ch.alni.certblues.azure.provision.AzureHttpChallengeProvisioner;
import ch.alni.certblues.storage.CertificateOrder;
import ch.alni.certblues.storage.CertificateRequest;
import ch.alni.certblues.storage.StorageService;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

/**
 * First function to test.
 */
@SuppressWarnings("unused")
public class Function {

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
        ).buildFor(request);
    }

    private static HttpChallengeProvisioner toHttpChallengeProvisioner(CertificateRequest request) {
        return new AzureHttpChallengeProvisioner(CONTEXT.getHttpClient(), request.storageEndpointUrl());
    }

    @FunctionName("submitOrders")
    public void submitOrders(@TimerTrigger(name = "keepAlive", schedule = "%Schedule%") String timerInfo,
                             ExecutionContext context) {

        context.getLogger().info("Order submission started: " + timerInfo);

        final TokenCredential credential = CONTEXT.getCredential();
        final Configuration configuration = CONTEXT.getConfiguration();
        final AcmeSessionFacade sessionFacade = CONTEXT.getSessionFacade();
        final StorageService storageService = CONTEXT.getStorageService();

        storageService.getCertificateRequests()
                .flatMap(certificateRequest -> {
                    final var orderRequest = toOrderRequest(certificateRequest);
                    final var httpChallengeProvisioner = toHttpChallengeProvisioner(certificateRequest);
                    return sessionFacade
                            // provision the order
                            .provisionOrder(orderRequest, null, httpChallengeProvisioner)
                            // submit the challenges and create a certificate order
                            .flatMap(provisionedOrder -> sessionFacade
                                    .submitChallenges(
                                            provisionedOrder.orderResource().getResourceUrl(),
                                            provisionedOrder.challenges())
                                    .map(challenges -> CertificateOrder.builder()
                                            .certificateRequest(certificateRequest)
                                            .challengeUrls(challenges.stream().map(Challenge::url).collect(Collectors.toList()))
                                            .orderUrl(provisionedOrder.orderResource().getResourceUrl())
                                            .build()))
                            // return the tuple (request, order)
                            .map(certificateOrder -> Tuples.of(certificateRequest, certificateOrder));
                })
                // at the end first remove the request, then store the order
                .flatMap(tuple -> storageService.remove(tuple.getT1())
                        .then(storageService.store(tuple.getT2()))
                        .then(Mono.just(tuple.getT2()))
                )
                .subscribe(
                        order -> context.getLogger().info("Order submitted: " + order),
                        throwable -> context.getLogger().log(Level.SEVERE, "cannot submit the order", throwable)
                );

        context.getLogger().info("Order submission completed");
    }

    @FunctionName("checkOrders")
    public void checkOrders(@TimerTrigger(name = "keepAlive", schedule = "%Schedule%") String timerInfo,
                            ExecutionContext context) {

        context.getLogger().info("Order verification started: " + timerInfo);

        final TokenCredential credential = CONTEXT.getCredential();
        final Configuration configuration = CONTEXT.getConfiguration();
        final AcmeSessionFacade sessionFacade = CONTEXT.getSessionFacade();
        final StorageService storageService = CONTEXT.getStorageService();
        final HttpClient httpClient = CONTEXT.getHttpClient();

        storageService.getCertificateOrders()
                .flatMap(certificateOrder -> sessionFacade.checkOrder(
                                certificateOrder.orderUrl(), toCertificateEntry(certificateOrder.certificateRequest()))
                        // check if valid (certificate is issued and uploaded)
                        .filter(order -> order.status() == OrderStatus.VALID)
                        // then return certificate order
                        .map(order -> certificateOrder))
                // at the end remove the completed certificate order
                .flatMap(certificateOrder -> storageService.remove(certificateOrder).then(Mono.just(certificateOrder)))
                .subscribe(
                        certificateOrder -> context.getLogger().info("Order completed: " + certificateOrder),
                        throwable -> context.getLogger().log(Level.SEVERE, "cannot submit the order", throwable)
                );

        context.getLogger().info("Order verification completed");
    }
}
