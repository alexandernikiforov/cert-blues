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

import java.time.Duration;

import ch.alni.certblues.acme.facade.AcmeClient;
import ch.alni.certblues.acme.facade.AcmeSessionFacade;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.azure.keyvault.AzureKeyVaultKey;
import ch.alni.certblues.azure.queue.AzureQueue;
import ch.alni.certblues.storage.StorageService;
import ch.alni.certblues.storage.impl.StorageServiceImpl;
import ch.alni.certblues.storage.queue.Queue;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.logging.LogLevel;
import reactor.netty.http.HttpProtocol;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

/**
 * Context for running the function application.
 */
public final class Context {

    private static final Context INSTANCE = create();

    private com.azure.core.http.HttpClient httpClient;
    private AcmeSessionFacade sessionFacade;
    private TokenCredential credential;
    private SigningKeyPair accountKeyPair;
    private StorageService storageService;
    private Configuration configuration;

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
                .protocol(HttpProtocol.HTTP11, HttpProtocol.H2)
                .runOn(new NioEventLoopGroup(2))
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL)
                .responseTimeout(Duration.ofSeconds(30));

        context.credential = new DefaultAzureCredentialBuilder().build();
        context.accountKeyPair = new AzureKeyVaultKey(
                context.credential, configuration.accountKeyId(), configuration.accountKeySignatureAlg()
        );
        final var acmeClient = new AcmeClient(baseHttpClient, configuration.directoryUrl());

        context.sessionFacade = new AcmeSessionFacade(acmeClient, context.accountKeyPair);
        context.httpClient = new NettyAsyncHttpClientBuilder(baseHttpClient).build();

        final Queue requestQueue = new AzureQueue(
                context.credential, context.httpClient, configuration.queueServiceUrl(), configuration.requestQueueName()
        );

        final Queue orderQueue = new AzureQueue(
                context.credential, context.httpClient, configuration.queueServiceUrl(), configuration.orderQueueName()
        );
        context.storageService = new StorageServiceImpl(requestQueue, orderQueue);
        return context;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public AcmeSessionFacade getSessionFacade() {
        return sessionFacade;
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
}
