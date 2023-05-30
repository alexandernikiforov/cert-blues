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

package ch.alni.certblues;

import ch.alni.certblues.acme.facade.AcmeClient;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.acme.protocol.AccountRequest;
import ch.alni.certblues.certbot.CertBot;
import ch.alni.certblues.certbot.CertificateStore;
import ch.alni.certblues.certbot.StorageService;
import ch.alni.certblues.certbot.impl.CertBotFactory;
import org.slf4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Entry point for the application.
 */
@Component
public class Runner implements CommandLineRunner {
    private static final Logger LOG = getLogger(Runner.class);

    private final CertBotFactory certBotFactory;
    private final StorageService storageService;
    private final CertificateStore certificateStore;
    private final SigningKeyPair accountKeyPair;
    private final AcmeClient acmeClient;
    private final CertBluesProperties properties;

    public Runner(CertBotFactory certBotFactory,
                  StorageService storageService,
                  CertificateStore certificateStore,
                  SigningKeyPair accountKeyPair,
                  AcmeClient acmeClient,
                  CertBluesProperties properties) {
        this.certBotFactory = certBotFactory;
        this.storageService = storageService;
        this.certificateStore = certificateStore;
        this.accountKeyPair = accountKeyPair;
        this.acmeClient = acmeClient;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        LOG.info("Certificate request processing started");

        final var accountRequest = AccountRequest.builder().termsOfServiceAgreed(true).build();
        final var acmeSession = acmeClient.login(accountKeyPair, accountRequest);
        final CertBot certBot = certBotFactory.create(acmeSession);

        // read available certificates
        final Duration renewalInterval = properties.getRenewalInterval();

        certificateStore.getExpiringCertificates(renewalInterval)
                .collectList().flux()
                // check each request against the list of available certificates
                .flatMap(storageService::getPendingCertificateRequests)
                .doOnNext(request -> LOG.info("certificate request found {}", request))
                // and then pass each remaining request to the certbot
                .flatMap(request -> certBot.submit(request)
                        .then(storageService.reset(request))
                        .then(Mono.just(request)))
                .onErrorStop()
                .doOnError(e -> LOG.error("error while processing certificate request", e))
                .doOnNext(request -> LOG.info("certificate request processed {}", request))
                .doOnComplete(() -> LOG.info("no more certificates requests found"))
                .blockLast(properties.getMaxExecutionTime());

        LOG.info("Certificate request processing ended");
    }

}
