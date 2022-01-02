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

import ch.alni.certblues.acme.client.request.CreatedResource;
import ch.alni.certblues.acme.client.request.NonceSource;
import ch.alni.certblues.acme.client.request.RequestHandler;
import ch.alni.certblues.acme.protocol.Order;
import ch.alni.certblues.acme.protocol.OrderFinalizationRequest;
import ch.alni.certblues.acme.protocol.OrderRequest;
import reactor.core.publisher.Mono;

public class OrderAccessor {

    private final NonceSource nonceSource;
    private final PayloadSigner payloadSigner;
    private final RetryHandler retryHandler;
    private final RequestHandler requestHandler;

    public OrderAccessor(NonceSource nonceSource,
                         PayloadSigner payloadSigner,
                         RetryHandler retryHandler, RequestHandler requestHandler) {
        this.nonceSource = nonceSource;
        this.payloadSigner = payloadSigner;
        this.retryHandler = retryHandler;
        this.requestHandler = requestHandler;
    }

    /**
     * Creates a new order.
     *
     * @param accountUrl      URL of the account
     * @param newOrderUrl     URL to create a new order
     * @param resourceRequest order creation request
     * @return mono over the created order (combined with the URL pointing to the created order)
     */
    public Mono<CreatedResource<Order>> createOrder(String accountUrl, String newOrderUrl, OrderRequest resourceRequest) {
        return nonceSource.getNonce()
                .flatMap(nonce -> payloadSigner.sign(newOrderUrl, accountUrl, resourceRequest, nonce))
                .flatMap(jwsObject -> requestHandler.create(newOrderUrl, jwsObject, nonceSource, Order.class))
                .retryWhen(retryHandler.getRetry());
    }

    /**
     * Loads the order by the given order URL.
     *
     * @param accountUrl URL of the account
     * @param orderUrl   URL of the order to load
     * @return mono of the order
     */
    public Mono<Order> getOrder(String accountUrl, String orderUrl) {
        return nonceSource.getNonce()
                .flatMap(nonce -> payloadSigner.sign(orderUrl, accountUrl, "", nonce))
                .flatMap(jwsObject -> requestHandler.request(orderUrl, jwsObject, nonceSource, Order.class))
                .retryWhen(retryHandler.getRetry());
    }

    /**
     * Submits CSR for the given order.
     *
     * @param accountUrl          URL of the account
     * @param finalizeUrl         URL to submit the CSR thus finalizing the order
     * @param finalizationRequest certificate sign request (DER-form base64-url-encoded)
     * @return mono over the refreshed order object
     */
    public Mono<Order> submitCsr(String accountUrl, String finalizeUrl, OrderFinalizationRequest finalizationRequest) {
        return nonceSource.getNonce()
                .flatMap(nonce -> payloadSigner.sign(finalizeUrl, accountUrl, finalizationRequest, nonce))
                .flatMap(jwsObject -> requestHandler.request(finalizeUrl, jwsObject, nonceSource, Order.class))
                .retryWhen(retryHandler.getRetry());
    }

    /**
     * Submits CSR for the given order.
     *
     * @param accountUrl     URL of the account
     * @param certificateUrl URL to download the certificate (chain) in PEM format
     * @return mono over the download certificate
     */
    public Mono<String> downloadCertificate(String accountUrl, String certificateUrl) {
        return nonceSource.getNonce()
                .flatMap(nonce -> payloadSigner.sign(certificateUrl, accountUrl, "", nonce))
                .flatMap(jwsObject -> requestHandler.request(certificateUrl, jwsObject, nonceSource))
                .retryWhen(retryHandler.getRetry());
    }
}
