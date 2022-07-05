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

package ch.alni.certblues.certbot.impl;

import org.springframework.stereotype.Component;

import ch.alni.certblues.acme.facade.AcmeSession;
import ch.alni.certblues.certbot.AuthorizationProvisionerFactory;
import ch.alni.certblues.certbot.CertBot;
import ch.alni.certblues.certbot.CertificateStore;

@Component
public class CertBotFactory {

    private final CertificateStore certificateStore;

    private final AuthorizationProvisionerFactory provisionerFactory;

    public CertBotFactory(CertificateStore certificateStore, AuthorizationProvisionerFactory provisionerFactory) {
        this.certificateStore = certificateStore;
        this.provisionerFactory = provisionerFactory;
    }

    /**
     * Creates a new cert bot with the given session against the ACME server.
     */
    public CertBot create(AcmeSession session) {
        return new CertBotImpl(session, certificateStore, provisionerFactory);
    }
}
