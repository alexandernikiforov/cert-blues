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
import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;

import java.security.KeyStore;
import java.time.Duration;

import javax.net.ssl.TrustManagerFactory;

import ch.alni.certblues.acme.facade.AcmeClient;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.azure.keyvault.AzureKeyVaultCertificateBuilder;
import ch.alni.certblues.azure.keyvault.AzureKeyVaultKey;
import ch.alni.certblues.azure.provision.AzureHttpChallengeProvisioner;
import ch.alni.certblues.azure.queue.AzureQueue;
import ch.alni.certblues.storage.StorageService;
import ch.alni.certblues.storage.certbot.AuthorizationProvisionerFactory;
import ch.alni.certblues.storage.certbot.CertificateRequest;
import ch.alni.certblues.storage.certbot.DnsChallengeProvisioner;
import ch.alni.certblues.storage.certbot.HttpChallengeProvisioner;
import ch.alni.certblues.storage.impl.StorageServiceImpl;
import ch.alni.certblues.storage.queue.Queue;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import reactor.netty.http.HttpProtocol;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

/**
 * Context for running the function application.
 */
public final class Context {

    private static final Context INSTANCE = create();

    private com.azure.core.http.HttpClient httpClient;
    private TokenCredential credential;
    private SigningKeyPair accountKeyPair;
    private StorageService storageService;
    private Configuration configuration;
    private AcmeClient acmeClient;
    private AzureKeyVaultCertificateBuilder certificateEntryFactory;
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

        final var baseHttpClient = configuration.staging() ?
                // add special SSL context to accept untrusted certificates in a staging environment
                reactor.netty.http.client.HttpClient.create(connectionProvider)
                        .secure(spec -> spec.sslContext(sslContext())) :
                reactor.netty.http.client.HttpClient.create(connectionProvider)
                        .protocol(HttpProtocol.HTTP11, HttpProtocol.H2)
                        .runOn(new NioEventLoopGroup(2))
                        .wiretap("reactor.netty.http.client.HttpClient", LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL)
                        .responseTimeout(Duration.ofSeconds(30));

        if (configuration.staging()) {
            baseHttpClient.secure(spec -> spec.sslContext(sslContext()));
        }

        context.credential = new DefaultAzureCredentialBuilder().build();
        context.accountKeyPair = new AzureKeyVaultKey(
                context.credential, configuration.accountKeyId(), configuration.accountKeySignatureAlg()
        );

        context.acmeClient = new AcmeClient(baseHttpClient, configuration.directoryUrl());
        context.httpClient = new NettyAsyncHttpClientBuilder(baseHttpClient).build();

        final Queue requestQueue = new AzureQueue(
                context.credential, context.httpClient, configuration.queueServiceUrl(), configuration.requestQueueName()
        );

        final Queue orderQueue = new AzureQueue(
                context.credential, context.httpClient, configuration.queueServiceUrl(), configuration.orderQueueName()
        );
        context.storageService = new StorageServiceImpl(requestQueue, orderQueue);

        context.certificateEntryFactory = new AzureKeyVaultCertificateBuilder(
                context.credential, context.httpClient, configuration.keyVaultUrl()
        );

        context.authorizationProvisionerFactory = new AuthorizationProvisionerFactory() {

            @Override
            public HttpChallengeProvisioner createHttpChallengeProvisioner(CertificateRequest certificateRequest) {
                return new AzureHttpChallengeProvisioner(context.httpClient, certificateRequest.storageEndpointUrl());
            }

            @Override
            public DnsChallengeProvisioner createDnsChallengeProvisioner(CertificateRequest certificateRequest) {
                // DNS provisioning is not supported
                return null;
            }
        };


        return context;
    }

    private static SslContext sslContext() {
        try {
            final KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(Context.class.getResourceAsStream("/truststore-staging.p12"), "123456".toCharArray());

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

    public AzureKeyVaultCertificateBuilder getCertificateEntryFactory() {
        return certificateEntryFactory;
    }

    public AuthorizationProvisionerFactory getAuthorizationProvisionerFactory() {
        return authorizationProvisionerFactory;
    }
}
