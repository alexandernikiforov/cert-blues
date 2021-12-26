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

import com.google.common.base.Preconditions;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.security.keyvault.certificates.models.CertificateContentType;
import com.azure.security.keyvault.certificates.models.CertificateKeyType;
import com.azure.security.keyvault.certificates.models.CertificateKeyUsage;
import com.azure.security.keyvault.certificates.models.CertificatePolicy;
import com.azure.security.keyvault.certificates.models.CertificatePolicyAction;
import com.azure.security.keyvault.certificates.models.LifetimeAction;
import com.azure.security.keyvault.certificates.models.SubjectAlternativeNames;

import java.util.List;

import ch.alni.certblues.storage.CertificateRequest;
import ch.alni.certblues.storage.KeyType;

public final class AzureKeyVaultCertificateBuilder {

    private static final String SERVER_CERTIFICATE_KEY_USAGE = "1.3.6.1.5.5.7.3.1";

    private final TokenCredential credential;
    private final HttpClient httpClient;
    private final String vaultUrl;

    public AzureKeyVaultCertificateBuilder(TokenCredential credential, String vaultUrl) {
        this(credential, null, vaultUrl);
    }

    public AzureKeyVaultCertificateBuilder(TokenCredential credential, HttpClient httpClient, String vaultUrl) {
        this.credential = credential;
        this.httpClient = httpClient;
        this.vaultUrl = vaultUrl;
    }

    private static CertificateKeyType toCertificateKeyType(KeyType keyType) {
        return CertificateKeyType.fromString(keyType.toString());
    }

    public AzureKeyVaultCertificate buildFor(CertificateRequest certificateRequest) {
        Preconditions.checkNotNull(credential, "credential cannot be null");
        Preconditions.checkNotNull(vaultUrl, "vaultUrl cannot be null");
        Preconditions.checkNotNull(certificateRequest, "certificateRequest cannot be null");

        // Unknown is important here! It is a fixed value
        final CertificatePolicy certificatePolicy = new CertificatePolicy("Unknown", "CN=DefaultPolicy")
                .setCertificateTransparent(false)
                .setContentType(CertificateContentType.PKCS12)
                .setKeySize(certificateRequest.keySize())
                .setKeyType(toCertificateKeyType(certificateRequest.keyType()))
                .setSubjectAlternativeNames(new SubjectAlternativeNames().setDnsNames(
                        certificateRequest.dnsNames()
                ))
                .setKeyUsage(CertificateKeyUsage.KEY_ENCIPHERMENT, CertificateKeyUsage.DIGITAL_SIGNATURE)
                .setEnhancedKeyUsage(List.of(SERVER_CERTIFICATE_KEY_USAGE))
                .setValidityInMonths(certificateRequest.validityInMonths())
                .setLifetimeActions(new LifetimeAction(CertificatePolicyAction.EMAIL_CONTACTS).setLifetimePercentage(80));

        return new AzureKeyVaultCertificate(
                credential, httpClient, vaultUrl, certificateRequest.certificateName(), certificatePolicy
        );
    }
}
