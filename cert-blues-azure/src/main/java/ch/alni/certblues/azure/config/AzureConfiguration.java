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

package ch.alni.certblues.azure.config;


import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.alni.certblues.acme.facade.DnsChallengeProvisioner;
import ch.alni.certblues.acme.facade.HttpChallengeProvisioner;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.api.CertificateRequest;
import ch.alni.certblues.azure.keyvault.AzureKeyVaultCertificate;
import ch.alni.certblues.azure.keyvault.AzureKeyVaultKey;
import ch.alni.certblues.azure.provision.AuthenticatedDnsZoneManager;
import ch.alni.certblues.azure.provision.AzureDnsChallengeProvisioner;
import ch.alni.certblues.azure.provision.AzureHttpChallengeProvisioner;
import ch.alni.certblues.azure.queue.AzureQueue;
import ch.alni.certblues.certbot.AuthorizationProvisionerFactory;
import ch.alni.certblues.certbot.CertificateStore;
import ch.alni.certblues.storage.queue.Queue;

/**
 * Configures the Azure-based implementation of the components used by the cert bot.
 */
@Configuration
@EnableConfigurationProperties(AzureConfigurationProperties.class)
public class AzureConfiguration {

    private final AzureConfigurationProperties properties;

    public AzureConfiguration(AzureConfigurationProperties properties) {
        this.properties = properties;
    }

    @Bean
    public TokenCredential credential() {
        return new DefaultAzureCredentialBuilder().build();
    }

    @Bean
    public HttpClient httpClient(reactor.netty.http.client.HttpClient baseHttpClient) {
        return new NettyAsyncHttpClientBuilder(baseHttpClient).build();
    }

    @Bean
    public SigningKeyPair signingKeyPair(TokenCredential credential, HttpClient httpClient) {
        return new AzureKeyVaultKey(credential, httpClient,
                properties.getAccountKey().getId(), properties.getAccountKey().getSignatureAlg());
    }

    @Bean
    public Queue queue(TokenCredential credential, HttpClient httpClient) {
        return new AzureQueue(credential, httpClient,
                properties.getQueueStorage().getServiceUrl(), properties.getQueueStorage().getRequestQueueName()
        );
    }

    @Bean
    public CertificateStore certificateStore(TokenCredential credential, HttpClient httpClient) {
        return new AzureKeyVaultCertificate(credential, httpClient, properties.getKeyVault().getUrl());
    }

    @Bean
    public AuthorizationProvisionerFactory provisionerFactory(TokenCredential credential, HttpClient httpClient) {
        final AuthenticatedDnsZoneManager dnsZoneManager = new AuthenticatedDnsZoneManager(credential);

        return new AuthorizationProvisionerFactory() {

            @Override
            public HttpChallengeProvisioner createHttpChallengeProvisioner(CertificateRequest certificateRequest) {
                return new AzureHttpChallengeProvisioner(credential, httpClient,
                        certificateRequest.storageEndpointUrl());
            }

            @Override
            public DnsChallengeProvisioner createDnsChallengeProvisioner(CertificateRequest certificateRequest) {
                if (certificateRequest.dnsZone() != null && certificateRequest.dnsZoneResourceGroup() != null) {
                    return new AzureDnsChallengeProvisioner(
                            dnsZoneManager, certificateRequest.dnsZoneResourceGroup(), certificateRequest.dnsZone()
                    );
                }
                else {
                    // DNS challenges are not supported
                    return null;
                }
            }
        };
    }
}
