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

package ch.alni.certblues.certbot.certificate;

import ch.alni.certblues.certbot.CertificateInfo;
import ch.alni.certblues.certbot.CertificateRequest;
import ch.alni.certblues.certbot.CertificateStore;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.KeyPair;
import java.time.Duration;

/**
 * Simple implementation of the certificate store for test purposes.
 */
public class SimpleCertEntry implements CertificateStore {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SimpleCertEntry.class);

    private static final String CSR_SIGNING_ALG = "SHA256withRSA";

    private final KeyPair keyPair;

    public SimpleCertEntry(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    @Override
    public Mono<byte[]> createCsr(CertificateRequest certificateRequest) {
        try {
            final var p10Builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Principal(certificateRequest.subjectDn()), keyPair.getPublic()
            );

            final var csBuilder = new JcaContentSignerBuilder(CSR_SIGNING_ALG);
            final ContentSigner signer = csBuilder.build(keyPair.getPrivate());

            final GeneralName[] subjectAltNames = certificateRequest.dnsNames().stream()
                    .map(dnsName -> new GeneralName(GeneralName.dNSName, dnsName))
                    .toArray(GeneralName[]::new);

            final Extension[] extensions = new Extension[]{
                    Extension.create(Extension.subjectAlternativeName, true, new GeneralNames(subjectAltNames))
            };

            final var csr = p10Builder
                    .addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, new Extensions(extensions))
                    .build(signer);

            return Mono.just(csr.getEncoded());
        }
        catch (OperatorCreationException | IOException e) {
            LOG.error("cannot create CSR", e);
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Void> upload(String name, String certificate) {
        return Mono.empty();
    }

    @Override
    public Flux<CertificateInfo> getExpiringCertificates(Duration renewalInterval) {
        return Flux.empty();
    }
}
