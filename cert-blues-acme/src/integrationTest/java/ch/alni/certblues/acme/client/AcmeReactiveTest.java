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

package ch.alni.certblues.acme.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.TrustManagerFactory;

import ch.alni.certblues.acme.client.access.AccountAccessor;
import ch.alni.certblues.acme.client.access.AuthorizationAccessor;
import ch.alni.certblues.acme.client.access.AuthorizationHandler;
import ch.alni.certblues.acme.client.access.ChallengeAccessor;
import ch.alni.certblues.acme.client.access.DirectoryAccessor;
import ch.alni.certblues.acme.client.access.OrderAccessor;
import ch.alni.certblues.acme.client.access.PayloadSigner;
import ch.alni.certblues.acme.client.access.RetryHandler;
import ch.alni.certblues.acme.client.request.NonceSource;
import ch.alni.certblues.acme.client.request.RequestHandler;
import ch.alni.certblues.acme.key.SimpleRsaKeyEntry;
import ch.alni.certblues.acme.key.SimpleRsaKeyPair;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class AcmeReactiveTest {
    private static final Logger LOG = getLogger(AcmeReactiveTest.class);

    private static final int PEBBLE_MGMT_PORT = 14000;
    private static final int CHALL_TEST_SRV_MGMT_PORT = 8055;

    private final ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
            .maxConnections(10)
            .maxIdleTime(Duration.ofSeconds(45))
            .maxLifeTime(Duration.ofSeconds(60))
            .build();

    private final HttpClient httpClient = HttpClient.create(connectionProvider)
            .protocol(HttpProtocol.HTTP11)
            .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)
            .responseTimeout(Duration.ofSeconds(30))
            .keepAlive(true)
            .secure(spec -> spec.sslContext(sslContext()));

    @BeforeEach
    void setUp() {
        LOG.info("hello");
    }

    @Test
    void getDirectory() throws Exception {
        final String directoryUrl = String.format("https://localhost:%s/dir", PEBBLE_MGMT_PORT);
        final String httpChallengeUrl = String.format("http://localhost:%s/add-http01", CHALL_TEST_SRV_MGMT_PORT);
        final String dnsChallengeUrl = String.format("http://localhost:%s/set-txt", CHALL_TEST_SRV_MGMT_PORT);

        final var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        final var accountKeyPair = new SimpleRsaKeyPair(new SimpleRsaKeyEntry(keyPair));

        final var requestHandler = new RequestHandler(httpClient);

        final var nonceSource = new NonceSource();
        final var directoryAccessor = new DirectoryAccessor(requestHandler, directoryUrl);
        final var payloadSigner = new PayloadSigner(accountKeyPair);
        final var retryHandler = new RetryHandler(5);

        final var challengeProvisioner = new TestChallengeProvisioner(httpClient, httpChallengeUrl, dnsChallengeUrl);

        final var accountAccessor = new AccountAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);
        final var orderAccessor = new OrderAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);
        final var authorizationAccessor = new AuthorizationAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);
        final var authorizationHandler = new AuthorizationHandler(accountKeyPair, challengeProvisioner);
        final var challengeAccessor = new ChallengeAccessor(nonceSource, payloadSigner, retryHandler, requestHandler);

        final AccountRequest accountRequest = AccountRequest.builder().termsOfServiceAgreed(true).build();
        final OrderRequest orderRequest = OrderRequest.builder()
                .identifiers(List.of(
                        Identifier.builder().type("dns").value("*.testserver.com").build(),
                        Identifier.builder().type("dns").value("www.testserver.com").build()
                ))
                .build();

        // let's build the request to get the tuple (directory, account, accountUrl)
        final Mono<Directory> directoryMono = directoryAccessor.getDirectory()
                .doOnNext(directory -> requestHandler.updateNonce(directory.newNonce(), nonceSource))
                .share();
        final var accountResourceMono = directoryMono
                .flatMap(directory -> accountAccessor.getAccount(directory.newAccount(), accountRequest))
                .share();
        final var accountUrlMono = accountResourceMono.map(CreatedResource::getResourceUrl)
                .share();
        final var orderMono = Mono.zip(directoryMono, accountUrlMono)
                .flatMap(tuple -> orderAccessor.createOrder(tuple.getT2(), tuple.getT1().newOrder(), orderRequest))
                .share();

        final Scheduler scheduler = Schedulers.newParallel("challenge");

        final Mono<Void> authorizationMono = Mono.zip(accountUrlMono, orderMono.map(CreatedResource::getResource))
                // create requests to get the authorizations for each of the authorization URL in the order
                .map(tuple -> tuple.getT2().authorizations().stream().map(authorizationUrl -> authorizationAccessor
                                .getAuthorization(tuple.getT1(), authorizationUrl)
                                .flatMap(authorizationHandler::handle)
                                .flatMap(challenge -> challengeAccessor.submitChallenge(tuple.getT1(), challenge.url()))
                        )
                        .collect(Collectors.toList()))
                // for each challenge mono try to execute the authorization requests
                .flatMap(Mono::when);

        // submit request for authorizations
        final Order order = authorizationMono.then(Mono.zip(accountUrlMono, orderMono.map(CreatedResource::getResourceUrl))
                        .flatMap(tuple -> orderAccessor.getOrder(tuple.getT1(), tuple.getT2())))
                .log()
                .block();

        assertThat(order).isNotNull();
    }

    private SslContext sslContext() {
        try {
            final KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(getClass().getResourceAsStream("/truststore.p12"), "123456".toCharArray());

            final var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            return SslContextBuilder.forClient()
                    .sslProvider(SslProvider.JDK)
                    .trustManager(tmf)
                    .build();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
