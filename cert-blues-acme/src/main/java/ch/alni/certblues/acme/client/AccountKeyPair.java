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

/**
 * Account key pair is used to sign the ACME requests on behalf of an account holding a pair of keys.
 */
public interface AccountKeyPair {

    /**
     * Signs the following ACME request by wrapping it into a JWS and passing the public key as 'jwk' attribute in the
     * protected header.
     *
     * @param requestUri the URI this request should be sent to
     * @param request    ACME request
     * @param nonce      the nonce received from the server
     * @return the signed request
     */
    JwsObject sign(String requestUri, Object request, String nonce);

    /**
     * Signs the following ACME request by wrapping it into a JWS and using the 'kid' attribute in the protected
     * header.
     *
     * @param requestUri the URI this request should be sent to
     * @param keyId      the key ID to be used
     * @param request    ACME request
     * @param nonce      the nonce received from the server
     * @return the signed request
     */
    JwsObject sign(String requestUri, String keyId, Object request, String nonce);

}
