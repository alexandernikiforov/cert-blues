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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;

import ch.alni.certblues.acme.facade.AcmeClient;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.acme.protocol.AccountRequest;
import ch.alni.certblues.certbot.CertBot;
import ch.alni.certblues.certbot.CertificateRequest;
import ch.alni.certblues.certbot.KeyType;
import ch.alni.certblues.certbot.impl.CertBotFactory;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles(profiles = "dev")
class AcmeStagingTest {

    private final CertificateRequest certificateRequest = CertificateRequest.builder()
            .subjectDn("CN=test.cloudalni.com")
            .certificateName("cloudalni5")
            .dnsZone("cloudalni.com")
            .dnsZoneResourceGroup("mydomainnames")
            .storageEndpointUrl("https://cloudalnitest.blob.core.windows.net/$web")
            .keyType(KeyType.RSA)
            .keySize(2048)
            .dnsNames(List.of("test.cloudalni.com", "*.test.cloudalni.com"))
            .validityInMonths(12)
            .build();

    @Autowired
    private AcmeClient acmeClient;

    @Autowired
    private SigningKeyPair accountKeyPair;

    @Autowired
    private CertBotFactory certBotFactory;

    @Test
    void getDirectory() {
        final var accountRequest = AccountRequest.builder().termsOfServiceAgreed(true).build();
        final var session = acmeClient.login(accountKeyPair, accountRequest);

        final CertBot certBot = certBotFactory.create(session);

        // provision
        final String certificate = certBot.submit(certificateRequest).block(Duration.ofSeconds(120));
        assertThat(certificate).isNotNull();
    }
}
