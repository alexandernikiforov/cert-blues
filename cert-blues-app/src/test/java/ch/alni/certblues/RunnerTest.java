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

package ch.alni.certblues;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import ch.alni.certblues.api.CertificateRequest;
import ch.alni.certblues.api.KeyType;
import ch.alni.certblues.certbot.CertificateInfo;

import static org.assertj.core.api.Assertions.assertThat;

class RunnerTest {

    public static final String CERTIFICATE_NAME_1 = "certificateName1";
    public static final String CERTIFICATE_NAME_2 = "certificateName2";
    private static final CertificateRequest CERTIFICATE_REQUEST = CertificateRequest.builder()
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

    @Test
    void shouldBeIncluded() {
        final Instant now = Instant.now();
        final Instant in20days = now.plus(20, ChronoUnit.DAYS);

        final List<CertificateInfo> certificateInfos = List.of(
                CertificateInfo.builder()
                        .expiresOn(in20days.plus(1, ChronoUnit.DAYS))
                        .certificateName(CERTIFICATE_NAME_1)
                        .build(),
                CertificateInfo.builder()
                        .expiresOn(in20days.minus(1, ChronoUnit.DAYS))
                        .certificateName(CERTIFICATE_NAME_2)
                        .build()
        );

        final CertificateRequest request1 = CERTIFICATE_REQUEST.toBuilder()
                .certificateName(CERTIFICATE_NAME_1)
                .build();

        final CertificateRequest request2 = CERTIFICATE_REQUEST.toBuilder()
                .certificateName(CERTIFICATE_NAME_2)
                .build();

        final CertificateRequest request3 = CERTIFICATE_REQUEST.toBuilder()
                .certificateName(CERTIFICATE_NAME_1)
                .forceRequestCreation(true)
                .build();

        assertThat(Runner.shouldBeIncluded(request1, certificateInfos, in20days)).isFalse();
        assertThat(Runner.shouldBeIncluded(request2, certificateInfos, in20days)).isTrue();
        assertThat(Runner.shouldBeIncluded(request3, certificateInfos, in20days)).isTrue();
    }
}
