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

package ch.alni.certblues.acme.pebble;

import java.net.URI;

import ch.alni.certblues.acme.facade.DnsChallengeProvisioner;
import ch.alni.certblues.acme.facade.HttpChallengeProvisioner;
import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;

/**
 * Pebble-based implementation of the challenge provisioner.
 */
public class TestChallengeProvisioner implements HttpChallengeProvisioner, DnsChallengeProvisioner {
    private final HttpClient httpClient;
    private final String addHttpUrl;
    private final String addDnsUrl;

    /**
     * Creates a new instance.
     *
     * @param httpClient the HTTP client to use
     * @param addHttpUrl the URL to call for HTTP challenge provisioning
     * @param addDnsUrl  the URL to call for DNS challenge provisioning
     */
    public TestChallengeProvisioner(HttpClient httpClient, String addHttpUrl, String addDnsUrl) {
        this.httpClient = httpClient;
        this.addHttpUrl = addHttpUrl;
        this.addDnsUrl = addDnsUrl;
    }

    @Override
    public Mono<Void> provisionHttp(String token, String keyAuth) {
        final var payload = String.format("{\"token\":\"%s\", \"content\": \"%s\"}", token, keyAuth);
        return send(addHttpUrl, payload);
    }

    @Override
    public Mono<Void> provisionDns(String host, String value) {
        final var name = "_acme-challenge." + host + ".";
        final var payload = String.format("{\"host\":\"%s\", \"value\": \"%s\"}", name, value);
        return send(addDnsUrl, payload);
    }

    private Mono<Void> send(String url, String payload) {
        return httpClient
                .headers(headers -> headers.add(HttpHeaderNames.CONTENT_TYPE, "application/json"))
                .post()
                .uri(URI.create(url))
                .send(ByteBufMono.fromString(Mono.just(payload)))
                .response()
                .flatMap(response -> Mono.empty());
    }
}
