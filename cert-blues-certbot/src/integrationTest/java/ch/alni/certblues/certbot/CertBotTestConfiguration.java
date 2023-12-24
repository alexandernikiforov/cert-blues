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

package ch.alni.certblues.certbot;

import ch.alni.certblues.acme.facade.DnsChallengeProvisioner;
import ch.alni.certblues.acme.facade.HttpChallengeProvisioner;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.acme.key.rsa.SimpleRsaKeyEntry;
import ch.alni.certblues.acme.key.rsa.SimpleRsaKeyPair;
import ch.alni.certblues.certbot.certificate.SimpleCertEntry;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import javax.net.ssl.TrustManagerFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.time.Duration;

@Configuration
@ComponentScan
public class CertBotTestConfiguration {

    private static final int CHALL_TEST_SRV_MGMT_PORT = 8055;

    @Bean
    HttpClient httpClient() {
        final ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxIdleTime(Duration.ofSeconds(45))
                .maxLifeTime(Duration.ofSeconds(60))
                .build();

        return HttpClient.create(connectionProvider)
                .protocol(HttpProtocol.HTTP11, HttpProtocol.H2)
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL)
                .responseTimeout(Duration.ofSeconds(30))
                .secure(spec -> spec.sslContext(sslContext()));
    }

    @Bean
    AuthorizationProvisionerFactory provisionerFactory(HttpClient httpClient) {
        final String httpChallengeUrl = String.format("http://localhost:%s/add-http01", CHALL_TEST_SRV_MGMT_PORT);
        final String dnsChallengeUrl = String.format("http://localhost:%s/set-txt", CHALL_TEST_SRV_MGMT_PORT);

        final var challengeProvisioner = new TestChallengeProvisioner(httpClient, httpChallengeUrl, dnsChallengeUrl);
        return new AuthorizationProvisionerFactory() {
            @Override
            public HttpChallengeProvisioner createHttpChallengeProvisioner(CertificateRequest certificateRequest) {
                return challengeProvisioner;
            }

            @Override
            public DnsChallengeProvisioner createDnsChallengeProvisioner(CertificateRequest certificateRequest) {
                return challengeProvisioner;
            }
        };
    }

    @Bean
    SigningKeyPair signingKeyPair() throws Exception {
        final var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);

        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return new SimpleRsaKeyPair(new SimpleRsaKeyEntry(keyPair));
    }

    @Bean
    CertificateStore certificateStore() throws Exception {
        // the certificate request parameters (key size and algorithm) are not taken into account
        final var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);

        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return new SimpleCertEntry(keyPair);
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
