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
import com.azure.identity.DefaultAzureCredentialBuilder;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import ch.alni.certblues.acme.facade.AcmeClient;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.acme.protocol.AccountRequest;
import ch.alni.certblues.azure.keyvault.AzureKeyVaultCertificate;
import ch.alni.certblues.azure.keyvault.AzureKeyVaultKey;
import ch.alni.certblues.azure.provision.AzureHttpChallengeProvisioner;
import ch.alni.certblues.storage.KeyType;
import ch.alni.certblues.storage.certbot.AuthorizationProvisionerFactory;
import ch.alni.certblues.storage.certbot.CertBot;
import ch.alni.certblues.storage.certbot.CertificateRequest;
import ch.alni.certblues.storage.certbot.DnsChallengeProvisioner;
import ch.alni.certblues.storage.certbot.HttpChallengeProvisioner;
import ch.alni.certblues.storage.certbot.impl.CertBotImpl;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.logging.LogLevel;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import static org.assertj.core.api.Assertions.assertThat;

class AcmeStagingTest {
    private static final String DIRECTORY_URL = "https://acme-staging-v02.api.letsencrypt.org/directory";

    private static final String VAULT_URL = "https://cert-blues-dev.vault.azure.net";
    private static final String KEY_ID = VAULT_URL + "/keys/accountKey/eed9a4146fb347cebf2a3907cda95fe0";

    private final ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
            .maxIdleTime(Duration.ofSeconds(45))
            .maxLifeTime(Duration.ofSeconds(60))
            .build();

    private final HttpClient httpClient = HttpClient.create(connectionProvider)
            .runOn(new NioEventLoopGroup(2))
            .protocol(HttpProtocol.HTTP11)
            .wiretap("reactor.netty.http.client.HttpClient", LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL)
            .responseTimeout(Duration.ofSeconds(30))
            .secure();

    // initialize the Azure client
    private final TokenCredential credential = new DefaultAzureCredentialBuilder().build();
    private final SigningKeyPair accountKeyPair = new AzureKeyVaultKey(credential, KEY_ID, "RS256");
    private final AzureKeyVaultCertificate azureKeyVaultCertificate =
            new AzureKeyVaultCertificate(credential, VAULT_URL);

    private final CertificateRequest certificateRequest = CertificateRequest.builder()
            .subjectDn("CN=cloudalni.com")
            .certificateName("cloudalni3")
            .storageEndpointUrl("https://cloudalni.blob.core.windows.net/$web?sp=racwdl&st=2021-12-28T17:37:43Z&se=2022-03-31T23:37:43Z&spr=https&sv=2020-08-04&sr=c&sig=qL41RVxADQJKxGSCYIYQwe6otoMUJxHaEMoS9dlQ2NA%3D")
            .keyType(KeyType.RSA)
            .keySize(2048)
            .dnsNames(List.of("cloudalni.com", "www.cloudalni.com"))
            .validityInMonths(12)
            .build();

    @Test
    void getDirectory() {
        final var provisionerFactory = new AuthorizationProvisionerFactory() {
            @Override
            public HttpChallengeProvisioner createHttpChallengeProvisioner(CertificateRequest certificateRequest) {
                return new AzureHttpChallengeProvisioner(certificateRequest.storageEndpointUrl());
            }

            @Override
            public DnsChallengeProvisioner createDnsChallengeProvisioner(CertificateRequest certificateRequest) {
                return null;
            }
        };

        final var acmeClient = new AcmeClient(httpClient, DIRECTORY_URL);
        final var accountRequest = AccountRequest.builder().termsOfServiceAgreed(true).build();
        final var session = acmeClient.login(accountKeyPair, accountRequest);

        final CertBot certBot = new CertBotImpl(session, azureKeyVaultCertificate, provisionerFactory);

        // provision
        final String certificate = certBot.submit(certificateRequest).block(Duration.ofSeconds(120));
        assertThat(certificate).isNotNull();
    }
}
