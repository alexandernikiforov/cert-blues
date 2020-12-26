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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import ch.alni.certblues.acme.client.impl.AccountKeyPairBuilder;
import ch.alni.certblues.acme.client.impl.AcmeClientBuilder;
import ch.alni.certblues.acme.jws.KeyVaultEntry;
import ch.alni.certblues.acme.jws.SimpleRsaKeyVaultEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The test against the Pebble server running in a local Docker container.
 */
class PebbleBasedTest {
    private static final Logger LOG = getLogger(PebbleBasedTest.class);

    private static final int PEBBLE_MGMT_PORT = 14000;
    private static final int CHALL_TEST_SRV_MGMT_PORT = 8055;

    private final AcmeClient client = new AcmeClientBuilder()
            .setSslContext(getSslContext())
            .build();

    @Test
    public void testContainerStart() throws Exception {
        final String directoryUrl = String.format("https://localhost:%s/dir", PEBBLE_MGMT_PORT);

        final String challengeUrl = String.format("http://localhost:%s/add-http01", CHALL_TEST_SRV_MGMT_PORT);

        final DirectoryHandle directoryHandle = client.getDirectory(directoryUrl);

        final Directory directory = directoryHandle.getDirectory();
        assertThat(directory.meta()).isNotNull();

        // now create a new account
        final var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);

        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        final KeyVaultEntry keyVaultEntry = new SimpleRsaKeyVaultEntry(keyPair);

        final var accountKeyPair = new AccountKeyPairBuilder()
                .setAlgorithm("RS256")
                .setKeyVaultEntry(keyVaultEntry)
                .build();

        final var accountHandle = directoryHandle.getAccount(accountKeyPair, AccountRequest.builder()
                .termsOfServiceAgreed(true)
                .build());

        assertThat(accountHandle.getAccount()).isNotNull();

        final Account account = accountHandle.reloadAccount();
        assertThat(account.status()).isEqualTo(AccountStatus.VALID);

        final var orderHandle = accountHandle.createOrder(OrderRequest.builder()
                .identifiers(List.of(
                        Identifier.builder().type("dns").value("testserver.com").build()
                ))
                .build());

        assertThat(orderHandle.getOrder()).isNotNull();

        final var order = orderHandle.reloadOrder();

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);

        final List<AuthorizationHandle> authorizations = orderHandle.getAuthorizations();
        assertThat(authorizations).hasSize(1);

        final AuthorizationHandle authorizationHandle = authorizations.get(0);

        // create the key authorization
        final Challenge challenge = authorizationHandle.getAuthorization().getChallenge("http-01");
        final var keyAuth = authorizationHandle.getKeyAuthorization("http-01");

        submitChallenge(challengeUrl, challenge, keyAuth);

        authorizationHandle.provisionChallenge("http-01");

        // poll authorization at most 10 times
        for (int i = 0; i < 10; i++) {
            final Authorization authorization = authorizationHandle.reloadAuthorization();
            if (authorization.status() == AuthorizationStatus.VALID) {
                LOG.info("authorization validated");
                break;
            }

            Thread.sleep(1000);
        }

        assertThat(authorizationHandle.getAuthorization().status()).isEqualTo(AuthorizationStatus.VALID);

        accountHandle.deactivateAccount();
    }

    private void submitChallenge(String challengeUrl, Challenge challenge, String keyAuth) throws Exception {
        final HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        final String payload = String.format("{\"token\":\"%s\", \"content\": \"%s\"}",
                challenge.token(), keyAuth);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(challengeUrl))
                .header("Content-Type", "application/jose+json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        LOG.info("challenge submitted, response status is {}", response.statusCode());
    }

    private SSLContext getSslContext() {
        try {
            final KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(getClass().getResourceAsStream("/truststore.p12"), "123456".toCharArray());

            final var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
            return sslContext;
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
