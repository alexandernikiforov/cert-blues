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

package ch.alni.certblues.auth.impl;

import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import ch.alni.certblues.auth.ClientCredentials;
import ch.alni.certblues.auth.ConnectionOptions;
import ch.alni.certblues.auth.ErrorResponse;
import ch.alni.certblues.auth.ManagedIdentity;
import ch.alni.certblues.auth.TokenEndpointException;
import ch.alni.certblues.auth.TokenResponse;

import static org.slf4j.LoggerFactory.getLogger;

class TokenEndpointClient {
    private static final Logger LOG = getLogger(TokenEndpointClient.class);

    private final static String MANAGED_IDENTITY_URI = "http://169.254.169.254/metadata/identity/oauth2/token";
    private final static String CLIENT_CREDENTIALS_URI = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";

    private final ConnectionOptions options;
    private final HttpClient httpClient;

    TokenEndpointClient(ConnectionOptions options) {
        this.options = options;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(options.getConnectionTimeout())
                .build();
    }

    /**
     * Returns the token for the managed identity.
     */
    TokenResponse getToken(ManagedIdentity managedIdentity) {
        LOG.info("querying a token for the managed identity {}", managedIdentity);

        final Map<String, String> params = new HashMap<>();
        params.put("resource", managedIdentity.getResource());
        params.put("api-version", "2018-02-01");
        params.put("object_id", managedIdentity.getObjectId());
        params.put("client_id", managedIdentity.getClientId());
        params.put("mi_res_id", managedIdentity.getManagedIdentityResourceId());

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(Queries.create(MANAGED_IDENTITY_URI, params))
                .header("Metadata", "true")
                .GET()
                .timeout(options.getRequestTimeout())
                .build();

        return getTokenResponse(httpRequest);
    }

    /**
     * Returns the token for the client credentials.
     */
    TokenResponse getToken(ClientCredentials clientCredentials) {
        LOG.info("querying a token for the client credentials {}", clientCredentials);

        final String url = String.format(CLIENT_CREDENTIALS_URI, clientCredentials.getTenant());

        final Map<String, String> params = new HashMap<>();
        params.put("client_id", clientCredentials.getClientId());
        params.put("scope", clientCredentials.getScope());
        params.put("client_secret", clientCredentials.getClientSecret());
        params.put("grant_type", "client_credentials");

        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(Queries.toQueryString(params)))
                .timeout(options.getRequestTimeout())
                .build();

        return getTokenResponse(httpRequest);
    }

    private TokenResponse getTokenResponse(HttpRequest httpRequest) {
        final HttpResponse<String> response = Requests.withErrorHandling(
                () -> httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        );

        final int statusCode = response.statusCode();

        if (statusCode == 200) {
            LOG.info("token successfully returned: {}", response.body());
            // return the token
            return Payloads.deserialize(response.body(), TokenResponse.class);
        }
        else {
            LOG.info("error or unexpected response returned: {}", response.body());
            final ErrorResponse errorResponse = Payloads.deserialize(response.body(), ErrorResponse.class);
            if (statusCode == 404 || statusCode == 429 || (statusCode >= 500 && statusCode < 600)) {
                // too many requests, endpoint updating, or a transient error - may be retried
                throw new TokenEndpointException(errorResponse, true);
            }
            else {
                // unrecoverable error
                throw new TokenEndpointException(errorResponse, false);
            }
        }
    }
}
