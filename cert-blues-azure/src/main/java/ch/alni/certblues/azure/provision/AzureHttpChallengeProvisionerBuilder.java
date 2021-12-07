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
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

import ch.alni.certblues.acme.client.access.HttpChallengeProvisioner;
import reactor.core.publisher.Mono;

/**
 * Uses Azure services to implement the challenge provisioning for ACME.
 */
public class AzureHttpChallengeProvisionerBuilder implements HttpChallengeProvisioner {
    private static final String WELL_KNOWN_ACME_CHALLENGE = ".well_known/acme_challenge/";

    private final BlobContainerAsyncClient blobContainerClient;

    public AzureHttpChallengeProvisionerBuilder(TokenCredential credential, String containerEndpointUrl) {
        blobContainerClient = new BlobContainerClientBuilder()
                .credential(credential)
                .endpoint(containerEndpointUrl)
                .buildAsyncClient();
    }

    @Override
    public Mono<Void> provisionHttp(String token, String keyAuth) {
        final BlobAsyncClient blobClient = blobContainerClient.getBlobAsyncClient(WELL_KNOWN_ACME_CHALLENGE + token);
        return blobClient
                .upload(BinaryData.fromString(keyAuth), true)
                .then();
    }
}
