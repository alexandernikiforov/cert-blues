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

package ch.alni.certblues.acme.client.request;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import ch.alni.certblues.acme.client.Directory;
import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

public class DirectoryRequest {

    private final HttpClient httpClient;

    public DirectoryRequest(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Mono<Directory> getDirectory(String directoryUrl) {
        return httpClient
                .headers(headers -> headers.add(HttpHeaderNames.CONTENT_TYPE, "application/json"))
                .get()
                .uri(URI.create(directoryUrl))
                .responseSingle((response, bufMono) -> bufMono
                        .asString(StandardCharsets.UTF_8)
                        .zipWith(Mono.just(response)))
                .map(responseTuple2 -> Payloads.getPayload(responseTuple2.getT2(), responseTuple2.getT1(), Directory.class));
    }
}
