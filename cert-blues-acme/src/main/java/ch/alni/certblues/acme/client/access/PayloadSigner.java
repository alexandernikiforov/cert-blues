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

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import ch.alni.certblues.acme.client.AcmeRequest;
import ch.alni.certblues.acme.json.JsonObjects;
import ch.alni.certblues.acme.jws.JwsHeader;
import ch.alni.certblues.acme.jws.JwsObject;
import ch.alni.certblues.acme.key.AccountKeyPair;
import reactor.core.publisher.Mono;

/**
 * Signs the payloads according to the ACME protocol.
 */
public class PayloadSigner {
    private final AccountKeyPair keyPair;

    /**
     * Creates a new accessor object to get information out of the given key pair stored in a remote vault.
     *
     * @param keyPair the remote interface to the key pair stored in a vault
     */
    public PayloadSigner(AccountKeyPair keyPair) {
        this.keyPair = keyPair;
    }

    private static String encode(String content) {
        final var encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String toPayload(Object request) {
        if (request instanceof AcmeRequest) {
            return JsonObjects.serialize(request);
        }
        else if (request instanceof String) {
            return (String) request;
        }
        else {
            throw new IllegalArgumentException("Invalid payload type, it must be either AcmeRequest or String");
        }
    }

    /**
     * Signs the given request object using the nonce and the provided request URL.
     *
     * @param requestUrl the request URL be included into the protected header
     * @param request    the request object to be signed
     * @param nonce      the nonce to be included into the protected header
     * @return the JWS object including the encoded protected header, content and signature
     */
    public Mono<JwsObject> sign(String requestUrl, Object request, String nonce) {
        final Mono<String> protectedHeaderMono = keyPair.getPublicJwk()
                .map(publicJwk -> JwsHeader.builder()
                        .jwk(publicJwk)
                        .alg(keyPair.getAlgorithm())
                        .url(requestUrl)
                        .nonce(nonce)
                        .build())
                .map(jwsHeader -> encode(JsonObjects.serialize(jwsHeader)));

        final Mono<String> encodedPayloadMono = Mono.fromSupplier(() -> encode(toPayload(request)));

        return doSign(protectedHeaderMono, encodedPayloadMono);
    }

    /**
     * Signs the given request object using the nonce and the provided request URL together with the key ID.
     *
     * @param requestUrl the request URL be included into the protected header
     * @param keyId      the key ID be included into the protected header
     * @param request    the request object to be signed
     * @param nonce      the nonce to be included into the protected header
     * @return the JWS object including the encoded protected header, content and signature
     */
    public Mono<JwsObject> sign(String requestUrl, String keyId, Object request, String nonce) {
        // create the protected header
        final Mono<String> protectedHeaderMono = Mono.fromSupplier(() -> JwsHeader.builder()
                        .kid(keyId)
                        .alg(keyPair.getAlgorithm())
                        .url(requestUrl)
                        .nonce(nonce)
                        .build())
                .map(jwsHeader -> encode(JsonObjects.serialize(jwsHeader)));

        // prepare the content
        final Mono<String> encodedPayloadMono = Mono.fromSupplier(() -> encode(toPayload(request)));

        return doSign(protectedHeaderMono, encodedPayloadMono);
    }

    @NotNull
    private Mono<JwsObject> doSign(Mono<String> protectedHeaderMono, Mono<String> encodedPayloadMono) {
        // the content to be signed is "protectedHeader.encodedPayload"
        final Mono<String> signatureMono = Mono
                .zip(protectedHeaderMono, encodedPayloadMono,
                        (protectedHeader, encodedPayload) -> protectedHeader + "." + encodedPayload)
                .flatMap(keyPair::sign);

        return Mono.zip(protectedHeaderMono, encodedPayloadMono, signatureMono)
                .map(tuple -> JwsObject.builder()
                        .protectedHeader(tuple.getT1())
                        .payload(tuple.getT2())
                        .signature(tuple.getT3())
                        .build());
    }
}
