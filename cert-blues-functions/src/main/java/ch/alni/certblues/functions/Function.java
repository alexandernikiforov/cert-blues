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

import ch.alni.certblues.acme.facade.AcmeClient;
import ch.alni.certblues.acme.protocol.AccountRequest;
import ch.alni.certblues.storage.StorageService;
import ch.alni.certblues.storage.certbot.CertBot;
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

    @FunctionName("submitOrders")
    public void submitOrders(@TimerTrigger(name = "submitOrdersSchedule", schedule = "%Schedule%") String timerInfo,
                             ExecutionContext context) {

        context.getLogger().info("Order submission started: " + timerInfo);

        final StorageService storageService = CONTEXT.getStorageService();
        final AcmeClient acmeClient = CONTEXT.getAcmeClient();

        final var accountRequest = AccountRequest.builder().termsOfServiceAgreed(true).build();
        final var acmeSession = acmeClient.login(CONTEXT.getAccountKeyPair(), accountRequest);

        final CertBot certBot = new CertBotImpl(
                acmeSession, CONTEXT.getCertificateStore(), CONTEXT.getAuthorizationProvisionerFactory()
        );

        storageService.getCertificateRequests()
                // make the cert bot submit the request
                .flatMap(request -> certBot.submit(request)
                        .then(storageService.remove(request))
                        .then(Mono.just(request)))
                // at the end first remove the request, then store the order
                .onErrorContinue((e, request) -> LOG.error("error while processing certificate request", e))
                .subscribe(
                        request -> LOG.info("certificate request processed {}", request),
                        throwable -> LOG.error("cannot submit the order", throwable),
                        () -> LOG.info("order submission completed")
                );

        context.getLogger().info("Order submission completed");
    }
}
