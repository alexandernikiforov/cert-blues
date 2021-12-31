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

package ch.alni.certblues.storage;

import ch.alni.certblues.storage.certbot.CertificateOrder;
import ch.alni.certblues.storage.certbot.CertificateRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StorageService {

    /**
     * Store the given certificate order.
     *
     * @param certificateRequest
     * @return empty mono if completed
     */
    Mono<CertificateRequest> store(CertificateRequest certificateRequest);

    /**
     * Removes the given order so that it is not available anymore.
     *
     * @param certificateRequest
     * @return empty mono if completed
     */
    Mono<Void> remove(CertificateRequest certificateRequest);

    /**
     * Returns a flux over the pending certificate requests.
     *
     * @return
     */
    Flux<CertificateRequest> getCertificateRequests();

    /**
     * Returns a flux over the submitted certificate orders.
     *
     * @return
     */
    Flux<CertificateOrder> getCertificateOrders();

    /**
     * Submit the given certificate order. The originating request will not be available anymore.
     *
     * @param certificateOrder
     * @return empty mono if completed
     */
    Mono<CertificateOrder> store(CertificateOrder certificateOrder);

    /**
     * Removes the given order so that it is not available anymore.
     *
     * @param certificateOrder
     * @return empty mono if completed
     */
    Mono<Void> remove(CertificateOrder certificateOrder);
}
