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

package ch.alni.certblues.keyvault;

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.HttpResponseException;
import com.azure.security.keyvault.certificates.CertificateAsyncClient;
import com.azure.security.keyvault.certificates.CertificateClientBuilder;
import com.azure.security.keyvault.certificates.models.CertificateKeyType;
import com.azure.security.keyvault.certificates.models.CertificatePolicy;
import com.azure.security.keyvault.certificates.models.MergeCertificateOptions;

import org.slf4j.Logger;

import java.util.List;

import ch.alni.certblues.acme.cert.Certificates;
import ch.alni.certblues.acme.key.CertificateEntry;
import ch.alni.certblues.acme.key.SigningKeyPair;
import reactor.core.publisher.Mono;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the key vault certificate based on the Azure certificate from a KeyVault.
 */
public class AzureKeyVaultCertificate implements CertificateEntry {

    private static final Logger LOG = getLogger(AzureKeyVaultCertificate.class);

    private final CertificateAsyncClient client;
    private final String certificateName;
    private final CertificatePolicy certificatePolicy;
    private final TokenCredential credential;

    public AzureKeyVaultCertificate(TokenCredential credential,
                                    String keyVaultUrl,
                                    String certificateName, CertificatePolicy certificatePolicy) {
        this.credential = credential;
        this.client = new CertificateClientBuilder()
                .credential(credential)
                .vaultUrl(keyVaultUrl)
                .buildAsyncClient();

        this.certificateName = certificateName;
        this.certificatePolicy = certificatePolicy;
    }

    private static String toSignatureAlg(CertificateKeyType keyType) {
        if (keyType.equals(CertificateKeyType.RSA) || keyType.equals(CertificateKeyType.RSA_HSM)) {
            return "RS256";
        }
        else if (keyType.equals(CertificateKeyType.EC) || keyType.equals(CertificateKeyType.EC_HSM)) {
            return "ES256";
        }
        else {
            throw new IllegalArgumentException("unsupported key type " + keyType);
        }
    }

    private static boolean isCertificateCreated(Throwable throwable) {
        if (throwable instanceof HttpResponseException) {
            final var exception = (HttpResponseException) throwable;
            return exception.getResponse().getStatusCode() == 201;
        }
        else {
            return false;
        }
    }

    @Override
    public Mono<SigningKeyPair> getSigningKeyPair() {
        return client.getCertificate(certificateName)
                .map(keyVaultCertificateWithPolicy -> new AzureKeyVaultKey(
                        credential, keyVaultCertificateWithPolicy.getKeyId(), toSignatureAlg(keyVaultCertificateWithPolicy.getPolicy().getKeyType())
                ));
    }

    @Override
    public Mono<byte[]> createCsr() {
        final var beginCertificateFlux = client.beginCreateCertificate(certificateName, certificatePolicy);
        final var certificateOperationFlux = client.getCertificateOperation(certificateName);

        return beginCertificateFlux
                // try to return the currently running operation
                // if the attempt to create a new certificate version fails
                .onErrorResume(throwable -> certificateOperationFlux)
                .next()
                .map(response -> response.getValue().getCsr());
    }

    @Override
    public Mono<Void> upload(String certificateChain) {
        final List<byte[]> encodedCertificates = Certificates.getEncodedCertificates(certificateChain);
        return client.mergeCertificate(new MergeCertificateOptions(certificateName, encodedCertificates))
                // workaround around the bug in MS library (it reports an error on status code 201)
                .onErrorResume(AzureKeyVaultCertificate::isCertificateCreated, throwable -> Mono.empty())
                .then();
    }
}
