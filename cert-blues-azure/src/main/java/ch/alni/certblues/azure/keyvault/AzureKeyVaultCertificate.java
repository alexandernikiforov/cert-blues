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

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpClient;
import com.azure.security.keyvault.certificates.CertificateAsyncClient;
import com.azure.security.keyvault.certificates.CertificateClientBuilder;
import com.azure.security.keyvault.certificates.models.CertificateContentType;
import com.azure.security.keyvault.certificates.models.CertificateKeyType;
import com.azure.security.keyvault.certificates.models.CertificateKeyUsage;
import com.azure.security.keyvault.certificates.models.CertificatePolicy;
import com.azure.security.keyvault.certificates.models.CertificatePolicyAction;
import com.azure.security.keyvault.certificates.models.CertificateProperties;
import com.azure.security.keyvault.certificates.models.LifetimeAction;
import com.azure.security.keyvault.certificates.models.MergeCertificateOptions;
import com.azure.security.keyvault.certificates.models.SubjectAlternativeNames;

import org.slf4j.Logger;

import java.util.List;

import ch.alni.certblues.certbot.CertificateInfo;
import ch.alni.certblues.certbot.CertificateRequest;
import ch.alni.certblues.certbot.CertificateStore;
import ch.alni.certblues.certbot.KeyType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the key vault certificate based on the Azure certificate from a KeyVault.
 */
public class AzureKeyVaultCertificate implements CertificateStore {

    private static final Logger LOG = getLogger(AzureKeyVaultCertificate.class);

    private static final String SERVER_CERTIFICATE_KEY_USAGE = "1.3.6.1.5.5.7.3.1";

    private final CertificateAsyncClient client;
    private final TokenCredential credential;

    public AzureKeyVaultCertificate(TokenCredential credential,
                                    HttpClient httpClient,
                                    String keyVaultUrl) {
        this.credential = credential;
        this.client = new CertificateClientBuilder()
                .credential(credential)
                .vaultUrl(keyVaultUrl)
                .httpClient(httpClient)
                .buildAsyncClient();
    }

    public AzureKeyVaultCertificate(TokenCredential credential, String keyVaultUrl) {
        this(credential, null, keyVaultUrl);
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

    private static CertificateKeyType toCertificateKeyType(KeyType keyType) {
        return CertificateKeyType.fromString(keyType.toString());
    }

    @Override
    public Mono<Void> upload(String certificateName, String certificateChain) {
        final List<byte[]> encodedCertificates = Certificates.getEncodedCertificates(certificateChain);

        final var certificateOperationFlux = client.getCertificateOperation(certificateName);
        final var mergeCertificateMono = client.mergeCertificate(new MergeCertificateOptions(certificateName, encodedCertificates))
                // workaround around the bug in MS library (it reports an error on status code 201)
                .onErrorResume(AzureKeyVaultCertificate::isCertificateCreated, throwable -> Mono.empty())
                .then();

        return certificateOperationFlux.next()
                .doOnError(throwable -> LOG.warn("error while checking if certificate operation is complete: " + throwable.getMessage()))
                .then(mergeCertificateMono)
                // disable the previous versions if any
                .then(disablePreviousVersionsMono(certificateName))
                .onErrorResume(throwable -> Mono.empty());
    }

    @Override
    public Flux<CertificateInfo> getCertificates() {
        return client.listPropertiesOfCertificates()
                .map(certificateProperties -> CertificateInfo.builder()
                        .certificateName(certificateProperties.getName())
                        .expiresOn(certificateProperties.getExpiresOn().toInstant())
                        .build());
    }

    @Override
    public Mono<byte[]> createCsr(CertificateRequest certificateRequest) {
        final String certificateName = certificateRequest.certificateName();

        // Unknown is important here! It is a fixed value
        final CertificatePolicy certificatePolicy =
                new CertificatePolicy("Unknown", certificateRequest.subjectDn())
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

        final var beginCertificateFlux = client.beginCreateCertificate(certificateName, certificatePolicy);
        final var certificateOperationFlux = client.getCertificateOperation(certificateName);

        return beginCertificateFlux
                .doOnError(throwable -> LOG.error("error while trying to create a new certificate", throwable))
                // try to return the currently running operation
                // if the attempt to create a new certificate version fails
                .onErrorResume(throwable -> certificateOperationFlux)
                .next()
                .map(response -> response.getValue().getCsr());
    }

    private Mono<Void> disablePreviousVersionsMono(String certificateName) {
        return client.getCertificate(certificateName)
                .flatMap(current -> client.listPropertiesOfCertificateVersions(certificateName)
                        // select only enabled versions
                        .filter(CertificateProperties::isEnabled)
                        // exclude the current version
                        .filter(properties -> !properties.getVersion().equals(current.getProperties().getVersion()))
                        .flatMap(this::disableCertificateVersionMono)
                        .then()
                );
    }

    private Mono<CertificateProperties> disableCertificateVersionMono(CertificateProperties properties) {
        return client.updateCertificateProperties(properties.setEnabled(false))
                .doOnSuccess(certificate -> LOG.info("Successfully disabled version {} of certificate {}",
                        properties.getVersion(), properties.getName()))
                .doOnError(throwable -> LOG.error("Cannot disable version {} of certificate {}",
                        properties.getVersion(), properties.getName(), throwable))
                .then(Mono.just(properties))
                // suppress errors
                .onErrorResume(throwable -> Mono.just(properties));
    }
}
