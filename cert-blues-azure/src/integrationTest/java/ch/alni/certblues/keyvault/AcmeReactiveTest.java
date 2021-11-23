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

package ch.alni.certblues.keyvault;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.keys.cryptography.CryptographyAsyncClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.security.KeyStore;
import java.time.Duration;
import java.util.List;

import javax.net.ssl.TrustManagerFactory;

import ch.alni.certblues.acme.client.CreatedResource;
import ch.alni.certblues.acme.client.Identifier;
import ch.alni.certblues.acme.client.Order;
import ch.alni.certblues.acme.client.OrderRequest;
import ch.alni.certblues.acme.facade.AcmeClient;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.acme.pebble.TestChallengeProvisioner;
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

    private static final String KEY_ID = "https://cert-blues-dev.vault.azure.net/keys/accountKey/eed9a4146fb347cebf2a3907cda95fe0";

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

    // Azure SDK client builders accept the credential as a parameter
    private final CryptographyAsyncClient cryptographyClient = new CryptographyClientBuilder()
            .keyIdentifier(KEY_ID)
            .credential(credential)
            .buildAsyncClient();

    private final SigningKeyPair accountKeyPair = new AzureKeyVaultKey(cryptographyClient, "RS256");

    @BeforeEach
    void setUp() {
        LOG.info("hello");
    }

    @Test
    void getDirectory() {
        final String directoryUrl = String.format("https://localhost:%s/dir", PEBBLE_MGMT_PORT);
        final String httpChallengeUrl = String.format("http://localhost:%s/add-http01", CHALL_TEST_SRV_MGMT_PORT);
        final String dnsChallengeUrl = String.format("http://localhost:%s/set-txt", CHALL_TEST_SRV_MGMT_PORT);

        final var challengeProvisioner = new TestChallengeProvisioner(httpClient, httpChallengeUrl, dnsChallengeUrl);

        final var acmeClient = new AcmeClient(httpClient, accountKeyPair, challengeProvisioner);

        final OrderRequest orderRequest = OrderRequest.builder()
                .identifiers(List.of(
                        Identifier.builder().type("dns").value("*.testserver.com").build(),
                        Identifier.builder().type("dns").value("www.testserver.com").build()
                ))
                .build();

        // submit request for authorizations
        final Order order = acmeClient.createOrder(directoryUrl, orderRequest)
                .log()
                .map(CreatedResource::getResource)
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
