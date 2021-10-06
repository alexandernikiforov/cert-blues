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

import java.security.KeyStore;
import java.time.Duration;

import javax.net.ssl.TrustManagerFactory;

import ch.alni.certblues.acme.client.access.DirectoryAccessor;
import ch.alni.certblues.acme.client.access.NonceAccessor;
import ch.alni.certblues.acme.client.request.DirectoryRequest;
import ch.alni.certblues.acme.client.request.NonceRequest;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import static org.slf4j.LoggerFactory.getLogger;

class AcmeReactiveTest {
    private static final Logger LOG = getLogger(AcmeReactiveTest.class);

    private static final int PEBBLE_MGMT_PORT = 14000;

    private final HttpClient httpClient = HttpClient.create()
            .protocol(HttpProtocol.HTTP11)
            .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)
            .responseTimeout(Duration.ofMillis(30))
            .secure(spec -> spec.sslContext(sslContext()));

    @BeforeEach
    void setUp() {
        LOG.info("hello");
    }

    @Test
    void getDirectory() throws Exception {
        final String directoryUrl = String.format("https://localhost:%s/dir", PEBBLE_MGMT_PORT);
        final DirectoryRequest directoryRequest = new DirectoryRequest(httpClient);
        final NonceRequest nonceRequest = new NonceRequest(httpClient);

        final DirectoryAccessor directoryAccessor = new DirectoryAccessor(directoryRequest, directoryUrl);
        final NonceAccessor nonceAccessor = new NonceAccessor(nonceRequest, directoryAccessor);

        nonceAccessor.getNonceValues()
                .subscribeOn(Schedulers.newParallel("nonce-thread"))
                .log()
                .subscribe(nonce -> LOG.info("nonce in subscription {}", nonce));

        nonceAccessor.update();
        nonceAccessor.update();
        nonceAccessor.update();

        LOG.info("repeating the last nonce");

        nonceAccessor.getNonceValues()
                .subscribeOn(Schedulers.newParallel("nonce-thread-1"))
                .log()
                .subscribe(nonce -> LOG.info("repeated nonce in subscription {}", nonce));

        Thread.sleep(5000L);
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
