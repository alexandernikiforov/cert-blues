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

package ch.alni.certblues.app;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.security.KeyStore;
import java.time.Duration;
import java.util.List;

import javax.net.ssl.TrustManagerFactory;

import ch.alni.certblues.acme.facade.AcmeClient;
import ch.alni.certblues.acme.facade.DnsChallengeProvisioner;
import ch.alni.certblues.acme.facade.HttpChallengeProvisioner;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.acme.pebble.TestChallengeProvisioner;
import ch.alni.certblues.acme.protocol.AccountRequest;
import ch.alni.certblues.azure.keyvault.AzureKeyVaultCertificate;
import ch.alni.certblues.azure.keyvault.AzureKeyVaultKey;
import ch.alni.certblues.certbot.AuthorizationProvisionerFactory;
import ch.alni.certblues.certbot.CertBot;
import ch.alni.certblues.certbot.CertificateRequest;
import ch.alni.certblues.certbot.KeyType;
import ch.alni.certblues.certbot.impl.CertBotImpl;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
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

    private static final String VAULT_URL = "https://cert-blues-dev.vault.azure.net";
    private static final String KEY_ID = VAULT_URL + "/keys/accountKey/eed9a4146fb347cebf2a3907cda95fe0";

    private final ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
            .maxIdleTime(Duration.ofSeconds(45))
            .maxLifeTime(Duration.ofSeconds(60))
            .build();

    private final HttpClient httpClient = HttpClient.create(connectionProvider)
            .protocol(HttpProtocol.HTTP11, HttpProtocol.H2)
            .wiretap("reactor.netty.http.client.HttpClient", LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL)
            .responseTimeout(Duration.ofSeconds(30))
            .secure(spec -> spec.sslContext(sslContext()));

    // initialize the Azure client
    private final TokenCredential credential = new DefaultAzureCredentialBuilder().build();
    private final SigningKeyPair accountKeyPair = new AzureKeyVaultKey(credential, KEY_ID, "RS256");
    private final AzureKeyVaultCertificate azureKeyVaultCertificate =
            new AzureKeyVaultCertificate(credential, VAULT_URL);

    private final CertificateRequest certificateRequest = CertificateRequest.builder()
            .certificateName("test-server2")
            .storageEndpointUrl("does-not-matter")
            .keyType(KeyType.RSA)
            .keySize(2048)
            .subjectDn("CN=testserver2.com")
            .dnsNames(List.of("*.testserver2.com"))
            .validityInMonths(12)
            .build();

    @BeforeEach
    void setUp() {
        LOG.info("hello");
    }

    @Test
    void getCsr() {
        final byte[] csr1 = azureKeyVaultCertificate.createCsr(certificateRequest).block();
        final byte[] csr2 = azureKeyVaultCertificate.createCsr(certificateRequest).block();
        assertThat(csr1).isEqualTo(csr2);

        final String thumbprint1 = azureKeyVaultCertificate.getSigningKeyPair(certificateRequest.certificateName())
                .flatMap(SigningKeyPair::getPublicKeyThumbprint)
                .block();
        final String thumbprint2 = azureKeyVaultCertificate.getSigningKeyPair(certificateRequest.certificateName())
                .flatMap(SigningKeyPair::getPublicKeyThumbprint)
                .block();
        assertThat(thumbprint1).isEqualTo(thumbprint2);
    }

    @Test
    void getDirectory() {
        final String directoryUrl = String.format("https://localhost:%s/dir", PEBBLE_MGMT_PORT);
        final String httpChallengeUrl = String.format("http://localhost:%s/add-http01", CHALL_TEST_SRV_MGMT_PORT);
        final String dnsChallengeUrl = String.format("http://localhost:%s/set-txt", CHALL_TEST_SRV_MGMT_PORT);

        final var challengeProvisioner = new TestChallengeProvisioner(httpClient, httpChallengeUrl, dnsChallengeUrl);
        final var provisionerFactory = new AuthorizationProvisionerFactory() {
            @Override
            public HttpChallengeProvisioner createHttpChallengeProvisioner(CertificateRequest certificateRequest) {
                return challengeProvisioner;
            }

            @Override
            public DnsChallengeProvisioner createDnsChallengeProvisioner(CertificateRequest certificateRequest) {
                return challengeProvisioner;
            }
        };

        final var acmeClient = new AcmeClient(httpClient, directoryUrl);
        final var accountRequest = AccountRequest.builder().termsOfServiceAgreed(true).build();
        final var session = acmeClient.login(accountKeyPair, accountRequest);

        final CertBot certBot = new CertBotImpl(session, azureKeyVaultCertificate, provisionerFactory);

        // wait for certificate and upload it
        final String certificate = certBot.submit(certificateRequest).block(Duration.ofSeconds(60));
        assertThat(certificate).isNotNull();
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
