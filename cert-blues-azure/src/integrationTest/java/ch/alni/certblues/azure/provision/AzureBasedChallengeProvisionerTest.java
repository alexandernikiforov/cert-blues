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

package ch.alni.certblues.azure.provision;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

class AzureBasedChallengeProvisionerTest {

    private static final String STORAGE_ENDPOINT_URL = System.getenv("ENV_STORAGE_ENDPOINT_URL");
    private static final String RESOURCE_GROUP_NAME = "mydomainnames";
    private static final String DNS_ZONE = "cloudalni.com";

    @Test
    void provisionHttp() {
        final TokenCredential credential = new DefaultAzureCredentialBuilder().build();

        final AzureHttpChallengeProvisioner httpChallengeProvisioner =
                new AzureHttpChallengeProvisioner(credential, STORAGE_ENDPOINT_URL);

        final Mono<Void> provisionHttp = httpChallengeProvisioner.provisionHttp("test", "keyAuth3");
        provisionHttp.block();
    }

    @Test
    void provisionDns() {
        final TokenCredential credential = new DefaultAzureCredentialBuilder().build();

        final AzureDnsChallengeProvisioner dnsChallengeProvisioner =
                new AzureDnsChallengeProvisioner(credential, RESOURCE_GROUP_NAME, DNS_ZONE);

        final Mono<Void> provisionDns = dnsChallengeProvisioner.provisionDns("cloudalni.com", "test2");
        provisionDns.block();
    }
}
