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

package ch.alni.certblues.azure.keyvault;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Static utility to work with certificates.
 */
public final class Certificates {

    private Certificates() {
    }

    /**
     * Encodes the given string (certificate chain in PEM format) as list of certificates in DER encoding.
     *
     * @param certificateChain certificate chain in PEM format
     */
    public static List<byte[]> getEncodedCertificates(String certificateChain) {
        final List<byte[]> encodedCertificates = new ArrayList<>();
        try {
            final var certificateFactory = CertificateFactory.getInstance("X.509");
            final var is = new ByteArrayInputStream(certificateChain.getBytes(StandardCharsets.US_ASCII));
            final Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(is);
            for (Certificate certificate : certificates) {
                encodedCertificates.add(certificate.getEncoded());
            }
        }
        catch (CertificateException e) {
            throw new IllegalArgumentException("cannot read the provided certificate chain", e);
        }
        return encodedCertificates;
    }
}
