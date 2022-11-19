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

import java.time.Duration;

/**
 * Integration test for DNS provisioner.
 */
class AzureDnsChallengeProvisionerTest {

    private final TokenCredential credential = new DefaultAzureCredentialBuilder().build();
    private final AuthenticatedDnsZoneManager dnsZoneManager = new AuthenticatedDnsZoneManager(credential);

    private final AzureDnsChallengeProvisioner provisioner = new AzureDnsChallengeProvisioner(
            dnsZoneManager, "mydomainnames", "cloudalni.com"
    );

    @Test
    void provisionDns() {
        provisioner.provisionDns("test.cloudalni.com", "value1").block(Duration.ofSeconds(100));
        provisioner.provisionDns("test.cloudalni.com", "value2").block(Duration.ofSeconds(100));
        provisioner.provisionDns("test.cloudalni.com", "value3").block(Duration.ofSeconds(100));
        provisioner.provisionDns("test.cloudalni.com", "value4").block(Duration.ofSeconds(100));
        provisioner.provisionDns("test.cloudalni.com", "value5").block(Duration.ofSeconds(100));
        provisioner.provisionDns("test.cloudalni.com", "value6").block(Duration.ofSeconds(100));
    }
}
