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

package ch.alni.certblues.acme.client.access;

import ch.alni.certblues.acme.client.request.NonceSource;
import ch.alni.certblues.acme.client.request.RequestHandler;
import reactor.core.publisher.Mono;

public class CertificateAccessor {

    private final NonceSource nonceSource;
    private final PayloadSigner payloadSigner;
    private final RetryHandler retryHandler;
    private final RequestHandler requestHandler;

    public CertificateAccessor(NonceSource nonceSource,
                               PayloadSigner payloadSigner,
                               RetryHandler retryHandler, RequestHandler requestHandler) {
        this.nonceSource = nonceSource;
        this.payloadSigner = payloadSigner;
        this.retryHandler = retryHandler;
        this.requestHandler = requestHandler;
    }

    /**
     * Downloads the given certificate.
     *
     * @param accountUrl     URL of the account
     * @param certificateUrl URL of the certificate to download
     * @return mono of the certificate chain in PEM format
     */
    public Mono<String> getCertificate(String accountUrl, String certificateUrl) {
        return nonceSource.getNonce()
                .flatMap(nonce -> payloadSigner.sign(certificateUrl, accountUrl, "", nonce))
                .flatMap(jwsObject -> requestHandler.request(certificateUrl, jwsObject, nonceSource, String.class))
                .retryWhen(retryHandler.getRetry());
    }
}
