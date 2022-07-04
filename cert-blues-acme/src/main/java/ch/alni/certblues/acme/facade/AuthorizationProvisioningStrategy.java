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

import com.google.common.base.Preconditions;

/**
 * What provision types are supported.
 */
public final class AuthorizationProvisioningStrategy {

    private final HttpChallengeProvisioner httpChallengeProvisioner;
    private final DnsChallengeProvisioner dnsChallengeProvisioner;

    AuthorizationProvisioningStrategy(HttpChallengeProvisioner httpChallengeProvisioner,
                                      DnsChallengeProvisioner dnsChallengeProvisioner) {
        this.httpChallengeProvisioner = httpChallengeProvisioner;
        this.dnsChallengeProvisioner = dnsChallengeProvisioner;
    }

    public static AuthorizationProvisioningStrategy of(HttpChallengeProvisioner httpChallengeProvisioner) {
        Preconditions.checkNotNull(httpChallengeProvisioner);
        return new AuthorizationProvisioningStrategy(httpChallengeProvisioner, null);
    }

    public static AuthorizationProvisioningStrategy of(DnsChallengeProvisioner dnsChallengeProvisioner) {
        Preconditions.checkNotNull(dnsChallengeProvisioner);
        return new AuthorizationProvisioningStrategy(null, dnsChallengeProvisioner);
    }

    public static AuthorizationProvisioningStrategy of(HttpChallengeProvisioner httpChallengeProvisioner,
                                                       DnsChallengeProvisioner dnsChallengeProvisioner) {
        return new AuthorizationProvisioningStrategy(httpChallengeProvisioner, dnsChallengeProvisioner);
    }

    public HttpChallengeProvisioner getHttpChallengeProvisioner() {
        return httpChallengeProvisioner;
    }

    public DnsChallengeProvisioner getDnsChallengeProvisioner() {
        return dnsChallengeProvisioner;
    }

    public boolean isDnsProvisioningSupported() {
        return null != dnsChallengeProvisioner;
    }

    public boolean isHttpProvisioningSupported() {
        return null != httpChallengeProvisioner;
    }

}
