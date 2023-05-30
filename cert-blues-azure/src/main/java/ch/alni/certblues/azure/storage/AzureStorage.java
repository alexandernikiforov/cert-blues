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

package ch.alni.certblues.azure.storage;

import ch.alni.certblues.certbot.CertificateInfo;
import ch.alni.certblues.certbot.CertificateRequest;
import ch.alni.certblues.certbot.KeyType;
import ch.alni.certblues.certbot.StorageService;
import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.data.tables.TableAsyncClient;
import com.azure.data.tables.TableServiceAsyncClient;
import com.azure.data.tables.TableServiceClientBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class AzureStorage implements StorageService {

    private final TableAsyncClient requestTableClient;

    public AzureStorage(TokenCredential credential,
                        HttpClient httpClient,
                        String tableServiceUrl,
                        String requestTableName) {
        TableServiceAsyncClient tableServiceClient = new TableServiceClientBuilder()
                .credential(credential).httpClient(httpClient).endpoint(tableServiceUrl)
                .buildAsyncClient();

        requestTableClient = tableServiceClient.getTableClient(requestTableName);
    }

    public AzureStorage(TokenCredential credential,
                        String tableServiceUrl,
                        String requestTableName) {
        this(credential, null, tableServiceUrl, requestTableName);
    }

    private static List<String> toDnsNames(String value) {
        final StringTokenizer tokenizer = new StringTokenizer(value, ",");
        final List<String> result = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            final String zone = tokenizer.nextToken();
            result.add(zone);
        }
        return result;
    }

    static boolean shouldBeIncluded(CertificateRequest request, List<CertificateInfo> certificateInfos) {
        return request.forceRequestCreation() || certificateInfos.stream()
                .anyMatch(certificateInfo -> certificateInfo.certificateName().equals(request.certificateName()));
    }

    @Override
    public Mono<Void> reset(CertificateRequest certificateRequest) {
        return requestTableClient.getEntity("certificateRequest", certificateRequest.certificateName())
                .map(tableEntity -> tableEntity.addProperty("forceRequestCreation", true))
                .flatMap(requestTableClient::updateEntity)
                .then();
    }

    @Override
    public Flux<CertificateRequest> getPendingCertificateRequests(List<CertificateInfo> expiringCertificates) {
        return requestTableClient.listEntities()
                .map(tableEntity -> CertificateRequest.builder()
                        .certificateName(tableEntity.getRowKey())
                        .keySize((Integer) tableEntity.getProperty("keySize"))
                        .keyType(KeyType.valueOf((String) tableEntity.getProperty("keyType")))
                        .subjectDn((String) tableEntity.getProperty("subjectDn"))
                        .validityInMonths((Integer) tableEntity.getProperty("validityInMonths"))
                        .dnsNames(toDnsNames((String) tableEntity.getProperty("dnsNames")))
                        .dnsZoneResourceGroup((String) tableEntity.getProperty("dnsZoneResourceGroup"))
                        .dnsZone((String) tableEntity.getProperty("dnsZone"))
                        .storageEndpointUrl((String) tableEntity.getProperty("storageEndpointUrl"))
                        .forceRequestCreation((Boolean) tableEntity.getProperty("forceRequestCreation"))
                        .build())
                .filter(certificateRequest -> shouldBeIncluded(certificateRequest, expiringCertificates));
    }

}
