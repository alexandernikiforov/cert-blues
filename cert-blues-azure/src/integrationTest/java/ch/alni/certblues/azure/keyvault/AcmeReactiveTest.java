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

package ch.alni.certblues.azure.keyvault;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.certificates.models.CertificateContentType;
import com.azure.security.keyvault.certificates.models.CertificateKeyType;
import com.azure.security.keyvault.certificates.models.CertificateKeyUsage;
import com.azure.security.keyvault.certificates.models.CertificatePolicy;
import com.azure.security.keyvault.certificates.models.CertificatePolicyAction;
import com.azure.security.keyvault.certificates.models.LifetimeAction;
import com.azure.security.keyvault.certificates.models.SubjectAlternativeNames;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.security.KeyStore;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.TrustManagerFactory;

import ch.alni.certblues.acme.client.Challenge;
import ch.alni.certblues.acme.client.Identifier;
import ch.alni.certblues.acme.client.Order;
import ch.alni.certblues.acme.client.OrderRequest;
import ch.alni.certblues.acme.client.OrderStatus;
import ch.alni.certblues.acme.facade.AcmeClient;
import ch.alni.certblues.acme.facade.AcmeSessionFacade;
import ch.alni.certblues.acme.facade.ProvisionedOrder;
import ch.alni.certblues.acme.key.CertificateEntry;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.acme.pebble.TestChallengeProvisioner;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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

    // initialize the Azure client
    private final TokenCredential credential = new DefaultAzureCredentialBuilder().build();
    private final SigningKeyPair accountKeyPair = new AzureKeyVaultKey(credential, KEY_ID, "RS256");

    private final CertificatePolicy certificatePolicy = new CertificatePolicy("Unknown", "CN=DefaultPolicy")
            .setCertificateTransparent(false)
            .setContentType(CertificateContentType.PKCS12)
            .setKeySize(2048)
            .setKeyType(CertificateKeyType.RSA)
            .setSubjectAlternativeNames(new SubjectAlternativeNames().setDnsNames(List.of("www.testserver.com")))
            .setKeyUsage(CertificateKeyUsage.KEY_ENCIPHERMENT, CertificateKeyUsage.DIGITAL_SIGNATURE)
            .setEnhancedKeyUsage(List.of("1.3.6.1.5.5.7.3.1"))
            .setValidityInMonths(12)
            .setLifetimeActions(new LifetimeAction(CertificatePolicyAction.EMAIL_CONTACTS).setLifetimePercentage(80));

    private final CertificateEntry certificateEntry = new AzureKeyVaultCertificate(credential, VAULT_URL, "test-server", certificatePolicy);

    @BeforeEach
    void setUp() {
        LOG.info("hello");
    }

    @Test
    void getCsr() {
        final byte[] csr1 = certificateEntry.createCsr().block();
        final byte[] csr2 = certificateEntry.createCsr().block();
        assertThat(csr1).isEqualTo(csr2);

        final String thumbprint1 = certificateEntry.getSigningKeyPair()
                .flatMap(SigningKeyPair::getPublicKeyThumbprint)
                .block();
        final String thumbprint2 = certificateEntry.getSigningKeyPair()
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

        final var acmeClient = new AcmeClient(httpClient, directoryUrl);
        final var sessionFacade = new AcmeSessionFacade(acmeClient, challengeProvisioner, challengeProvisioner);

        final var orderRequest = OrderRequest.builder()
                .identifiers(List.of(
                        Identifier.builder().type("dns").value("www.testserver.com").build()
                ))
                .build();

        // create and provision order
        final ProvisionedOrder provisionedOrder = sessionFacade.provisionOrder(accountKeyPair, orderRequest)
                .log()
                .block();

        assertThat(provisionedOrder).isNotNull();
        final String orderUrl = provisionedOrder.orderResource().getResourceUrl();

        // submit challenges
        final Mono<List<Challenge>> submitChallengeMono =
                sessionFacade.submitChallenges(accountKeyPair, orderUrl, provisionedOrder.challenges());

        final List<Challenge> challengeList = submitChallengeMono.block();
        assertThat(challengeList).isNotEmpty();

        // create a CSR
        final String encodedCsr = certificateEntry.createCsr()
                .map(csr -> Base64.getUrlEncoder().withoutPadding().encodeToString(csr))
                .block();

        assertThat(encodedCsr).isNotNull();

        // submit the certificate request
        final Order finalOrder = Flux.interval(Duration.ofSeconds(2))
                .flatMap(unused -> sessionFacade.submitCsr(accountKeyPair, orderUrl, encodedCsr))
                .takeUntil(order -> order.status() != OrderStatus.PENDING && order.status() != OrderStatus.PROCESSING)
                .blockLast(Duration.ofSeconds(15));

        assertThat(finalOrder).isNotNull();
        assertThat(finalOrder.status()).isEqualByComparingTo(OrderStatus.VALID);

        // download the certificate and merge it into the key vault
        sessionFacade.downloadCertificate(accountKeyPair, finalOrder.certificate())
                .flatMap(certificateEntry::upload)
                .block();
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
