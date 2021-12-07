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

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import ch.alni.certblues.acme.client.CreatedResource;
import ch.alni.certblues.acme.json.JsonObjects;
import ch.alni.certblues.acme.jws.JwsObject;
import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.util.function.Tuple2;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Common functionality to send requests.
 */
public class RequestHandler {
    private static final Logger LOG = getLogger(RequestHandler.class);

    private final HttpClient httpClient;

    public RequestHandler(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Returns request to get a new nonce from the ACME server.
     *
     * @param newNonceUrl the URL to receive the nonce
     */
    public void updateNonce(String newNonceUrl, NonceSource nonceSource) {
        httpClient
                .head()
                .uri(URI.create(newNonceUrl))
                .response()
                .map(HttpResponses::getNonce)
                .filter(Objects::nonNull)
                .subscribe(
                        nonceSource::update,
                        throwable -> LOG.error("unexpected error while getting a new nonce", throwable)
                );
    }

    /**
     * Issues a GET request to get the resource at the provided URL.
     *
     * @param resourceUrl the URL pointing at the resource
     * @param clazz       the type of the resource object
     * @param <T>         the type parameter
     * @return mono over the returned resource
     */
    public <T> Mono<T> get(String resourceUrl, Class<T> clazz) {
        return httpClient
                .headers(headers -> headers.add(HttpHeaderNames.CONTENT_TYPE, "application/json"))
                .get()
                .uri(URI.create(resourceUrl))
                .responseSingle((response, bufMono) -> bufMono
                        .asString(StandardCharsets.UTF_8)
                        .zipWith(Mono.just(response)))
                .map(responseTuple2 -> HttpResponses.getPayload(responseTuple2.getT2(), responseTuple2.getT1(), clazz));
    }

    /**
     * Issues a request to get the resource at the provided URL.
     *
     * @param resourceUrl the URL pointing at the resource
     * @param jwsObject   the signed request payload encoded as JWS
     * @param clazz       the type of the resource object
     * @param <T>         the type parameter
     * @return mono over the returned resource
     */
    public <T> Mono<T> request(String resourceUrl, JwsObject jwsObject, NonceSource nonceSource, Class<T> clazz) {
        return doRequest(resourceUrl, jwsObject, nonceSource)
                .map(responseTuple2 -> HttpResponses.getPayload(responseTuple2.getT2(), responseTuple2.getT1(), clazz));
    }

    /**
     * Issues a request to get the resource at the provided URL.
     *
     * @param resourceUrl the URL pointing at the resource
     * @param jwsObject   the signed request payload encoded as JWS
     * @return mono over the returned resource
     */
    public Mono<String> request(String resourceUrl, JwsObject jwsObject, NonceSource nonceSource) {
        return doRequest(resourceUrl, jwsObject, nonceSource)
                .map(responseTuple2 -> HttpResponses.getPayload(responseTuple2.getT2(), responseTuple2.getT1()));
    }

    /**
     * Creates a new resource on the ACME server and returns it together with the URL pointing to this resource.
     *
     * @param newResourceUrl URL to call to create a new resource
     * @param jwsObject      the signed request payload encoded as JWS
     * @param clazz          the type of the resource to return
     * @param <T>            the type parameter
     * @return mono over the created resource
     */
    public <T> Mono<CreatedResource<T>> create(String newResourceUrl, JwsObject jwsObject, NonceSource nonceSource, Class<T> clazz) {
        return doRequest(newResourceUrl, jwsObject, nonceSource)
                .map(responseTuple2 -> new CreatedResource<>(
                        // resource object
                        HttpResponses.getPayload(responseTuple2.getT2(), responseTuple2.getT1(), clazz),
                        // location header pointing to it
                        HttpResponses.getLocation(responseTuple2.getT2())
                ));
    }

    @NotNull
    private Mono<Tuple2<String, HttpClientResponse>> doRequest(String resourceUrl, JwsObject jwsObject, NonceSource nonceSource) {
        return httpClient
                .headers(headers -> headers.add(HttpHeaderNames.CONTENT_TYPE, "application/jose+json"))
                .post()
                .uri(URI.create(resourceUrl))
                .send(ByteBufMono.fromString(Mono.fromSupplier(() -> JsonObjects.serialize(jwsObject))))
                .responseSingle((response, bufMono) -> bufMono
                        .asString(StandardCharsets.UTF_8)
                        .zipWith(Mono.just(response)))
                .doOnNext(responseTuple2 -> propagateNonce(HttpResponses.getNonce(responseTuple2.getT2()), nonceSource));
    }

    private void propagateNonce(String nonce, NonceSource nonceSource) {
        if (null != nonce) {
            nonceSource.update(nonce);
        }
    }
}
