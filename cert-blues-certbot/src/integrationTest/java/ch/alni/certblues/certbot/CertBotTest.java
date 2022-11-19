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

package ch.alni.certblues.certbot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.List;

import ch.alni.certblues.acme.protocol.AccountRequest;
import ch.alni.certblues.api.CertificateRequest;
import ch.alni.certblues.api.KeyType;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class CertBotTest extends CertBotTestSupport {

    private final CertificateRequest certificateRequest = CertificateRequest.builder()
            .certificateName("test-server2")
            .storageEndpointUrl("does-not-matter")
            .keyType(KeyType.RSA)
            .keySize(2048)
            .subjectDn("CN=testserver2.com")
            .dnsNames(List.of("*.testserver2.com", "testserver2.com"))
            .validityInMonths(12)
            .build();

    @Test
    void getCsr() {
        final byte[] csr1 = certificateStore.createCsr(certificateRequest).block();
        final byte[] csr2 = certificateStore.createCsr(certificateRequest).block();
        assertThat(csr1).isEqualTo(csr2);
    }

    /**
     * This test runs against Pebble installation.
     */
    @Test
    void testCertificateIssueWithPebble() {
        final var accountRequest = AccountRequest.builder().termsOfServiceAgreed(true).build();
        final var session = acmeClient.login(accountKeyPair, accountRequest);
        final CertBot certBot = certBotFactory.create(session);

        // wait for certificate and upload it
        final String certificate = certBot.submit(certificateRequest).block(Duration.ofSeconds(60));
        assertThat(certificate).isNotNull();
    }
}
