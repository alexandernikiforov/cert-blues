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

package ch.alni.certblues.acme.facade;

import ch.alni.certblues.acme.client.request.RequestHandler;
import ch.alni.certblues.acme.key.SigningKeyPair;
import ch.alni.certblues.acme.protocol.AccountRequest;
import ch.alni.certblues.acme.protocol.Directory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Client for an ACME server. A single client always points to the same directory on the same ACME server.
 */
public final class AcmeClient {

    private final RequestHandler requestHandler;
    private final Mono<Directory> directoryMono;

    /**
     * Creates a new instance.
     *
     * @param httpClient   HTTP client to be used
     * @param directoryUrl URL of the ACME server directory
     */
    public AcmeClient(HttpClient httpClient, String directoryUrl) {
        this.requestHandler = new RequestHandler(httpClient);
        this.directoryMono = requestHandler.get(directoryUrl, Directory.class).share();
    }

    public AcmeSession login(SigningKeyPair accountKeyPair, AccountRequest accountRequest) {
        return new AcmeSession(requestHandler, directoryMono, accountKeyPair, accountRequest);
    }
}
