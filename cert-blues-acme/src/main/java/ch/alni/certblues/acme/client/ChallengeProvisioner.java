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

package ch.alni.certblues.acme.client;

import reactor.core.publisher.Mono;

/**
 * Interface to provision ACME challenges.
 */
public interface ChallengeProvisioner {

    /**
     * Provisions HTTP challenge.
     *
     * @param token   the token of the challenge
     * @param keyAuth the calculated key authorization
     * @return mono that emits "true" when the challenge has been provisioned or error if the challenge cannot be
     * provisioned
     */
    Mono<Void> provisionHttp(String token, String keyAuth);

    /**
     * Provisions DNS challenge.
     *
     * @param host  the name of the TXT record to be created in the DNS zone
     * @param value the value of the TXT record to be created in the DNS zone
     * @return mono that emits "true" when the challenge has been provisioned or error if the challenge cannot be
     * provisioned
     */
    Mono<Void> provisionDns(String host, String value);
}