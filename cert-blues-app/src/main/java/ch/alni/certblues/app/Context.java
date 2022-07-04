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
import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;

import org.slf4j.Logger;

import java.time.Duration;

import ch.alni.certblues.acme.facade.AcmeClient;
import ch.alni.certblues.acme.facade.DnsChallengeProvisioner;
import ch.alni.certblues.acme.facade.HttpChallengeProvisioner;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.azure.keyvault.AzureKeyVaultCertificate;
import ch.alni.certblues.azure.keyvault.AzureKeyVaultKey;
import ch.alni.certblues.azure.provision.AzureDnsChallengeProvisioner;
import ch.alni.certblues.azure.provision.AzureHttpChallengeProvisioner;
import ch.alni.certblues.azure.queue.AzureQueue;
import ch.alni.certblues.certbot.AuthorizationProvisionerFactory;
import ch.alni.certblues.certbot.CertificateRequest;
import ch.alni.certblues.certbot.CertificateStore;
import ch.alni.certblues.certbot.StorageService;
import ch.alni.certblues.certbot.impl.StorageServiceImpl;
import ch.alni.certblues.certbot.queue.Queue;
import io.netty.handler.logging.LogLevel;
import reactor.netty.http.HttpProtocol;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Context for running the function application.
 */
public final class Context {
    private static final Logger LOG = getLogger(Context.class);

    private static final Context INSTANCE = create();

    private com.azure.core.http.HttpClient httpClient;
    private TokenCredential credential;
    private SigningKeyPair accountKeyPair;
    private StorageService storageService;
    private Configuration configuration;
    private AcmeClient acmeClient;
    private CertificateStore certificateStore;
    private AuthorizationProvisionerFactory authorizationProvisionerFactory;

    private Context() {
    }

    public static Context getInstance() {
        return INSTANCE;
    }

    private static Context create() {
        final Configuration configuration = new ConfigurationLoader().loadFromEnv();

        final Context context = new Context();
        context.configuration = configuration;

        final ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxIdleTime(Duration.ofSeconds(45))
                .maxLifeTime(Duration.ofSeconds(60))
                .build();

        final var baseHttpClient = reactor.netty.http.client.HttpClient.create(connectionProvider)
                .secure()
                .protocol(HttpProtocol.HTTP11, HttpProtocol.H2)
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL)
                .responseTimeout(Duration.ofSeconds(30));

        context.credential = new DefaultAzureCredentialBuilder().build();
        context.accountKeyPair = new AzureKeyVaultKey(
                context.credential, configuration.accountKeyId(), configuration.accountKeySignatureAlg()
        );

        context.acmeClient = new AcmeClient(baseHttpClient, configuration.directoryUrl());
        context.httpClient = new NettyAsyncHttpClientBuilder(baseHttpClient).build();

        final Queue requestQueue = new AzureQueue(
                context.credential, context.httpClient, configuration.queueServiceUrl(), configuration.requestQueueName()
        );

        context.storageService = new StorageServiceImpl(requestQueue);

        context.certificateStore = new AzureKeyVaultCertificate(
                context.credential, context.httpClient, configuration.keyVaultUrl()
        );

        context.authorizationProvisionerFactory = new AuthorizationProvisionerFactory() {

            @Override
            public HttpChallengeProvisioner createHttpChallengeProvisioner(CertificateRequest certificateRequest) {
                return new AzureHttpChallengeProvisioner(context.httpClient, certificateRequest.storageEndpointUrl());
            }

            @Override
            public DnsChallengeProvisioner createDnsChallengeProvisioner(CertificateRequest certificateRequest) {
                if (certificateRequest.dnsZone() != null && certificateRequest.dnsZoneResourceGroup() != null) {
                    return new AzureDnsChallengeProvisioner(
                            context.credential, certificateRequest.dnsZoneResourceGroup(), certificateRequest.dnsZone()
                    );
                }
                else {
                    // DNS challenges are not supported
                    return null;
                }
            }
        };

        return context;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public TokenCredential getCredential() {
        return credential;
    }

    public SigningKeyPair getAccountKeyPair() {
        return accountKeyPair;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public AcmeClient getAcmeClient() {
        return acmeClient;
    }

    public CertificateStore getCertificateStore() {
        return certificateStore;
    }

    public AuthorizationProvisionerFactory getAuthorizationProvisionerFactory() {
        return authorizationProvisionerFactory;
    }
}
