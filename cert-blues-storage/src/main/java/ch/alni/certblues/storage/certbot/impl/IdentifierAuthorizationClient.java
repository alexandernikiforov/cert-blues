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

package ch.alni.certblues.storage.certbot.impl;

import org.slf4j.Logger;

import ch.alni.certblues.acme.facade.AuthorizationProvisioner;
import ch.alni.certblues.acme.key.Thumbprints;
import ch.alni.certblues.acme.protocol.Authorization;
import ch.alni.certblues.acme.protocol.Challenge;
import ch.alni.certblues.acme.protocol.ChallengeStatus;
import ch.alni.certblues.acme.protocol.DnsChallenge;
import ch.alni.certblues.acme.protocol.HttpChallenge;
import ch.alni.certblues.acme.protocol.Identifier;
import ch.alni.certblues.storage.certbot.DnsChallengeProvisioner;
import ch.alni.certblues.storage.certbot.HttpChallengeProvisioner;
import reactor.core.publisher.Mono;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles authorization by preparing the key authorization and provisioning one of the challenges.
 */
public class IdentifierAuthorizationClient implements AuthorizationProvisioner {
    private static final Logger LOG = getLogger(IdentifierAuthorizationClient.class);

    private final String publicKeyThumbprint;
    private final HttpChallengeProvisioner httpChallengeProvisioner;
    private final DnsChallengeProvisioner dnsChallengeProvisioner;

    public IdentifierAuthorizationClient(String publicKeyThumbprint,
                                         HttpChallengeProvisioner httpChallengeProvisioner,
                                         DnsChallengeProvisioner dnsChallengeProvisioner
    ) {
        this.publicKeyThumbprint = publicKeyThumbprint;
        this.httpChallengeProvisioner = httpChallengeProvisioner;
        this.dnsChallengeProvisioner = dnsChallengeProvisioner;
    }

    /**
     * Calls the process of the identifier authorization. Returns a mono that emits when the client has handled this
     * authorization.
     *
     * @param authorization the authorization to handle
     * @return the mono of the challenge that has been provisioned
     */
    @Override
    public Mono<Challenge> process(Authorization authorization) {
        switch (authorization.status()) {
            case EXPIRED:
            case DEACTIVATED:
            case INVALID:
            case REVOKED:
                return Mono.error(new IllegalArgumentException("cannot proceed with authorization of status " +
                        authorization.status()));
            case VALID:
                // this authorization is already done
                final var submittedChallenge = selectChallenge(authorization);
                return Mono.just(submittedChallenge);
            case PENDING:
                final var challenge = selectChallenge(authorization);
                if (challenge.status() != ChallengeStatus.PENDING) {
                    // do not provision challenges that are not pending
                    return Mono.just(challenge);
                }
                final var keyAuth = challenge.token() + "." + publicKeyThumbprint;
                return provision(authorization.identifier(), challenge, keyAuth)
                        .then(Mono.just(challenge));
            default:
                throw new IllegalArgumentException("unsupported authorization status " + authorization.status());
        }
    }

    private Mono<Void> provision(Identifier identifier, Challenge challenge, String keyAuth) {
        if (challenge instanceof DnsChallenge && isDnsProvisioningSupported()) {
            final var name = identifier.value();
            final var value = Thumbprints.getSha256Digest(keyAuth);
            return dnsChallengeProvisioner.provisionDns(name, value);
        }
        else if (challenge instanceof HttpChallenge && isHttpProvisioningSupported()) {
            return httpChallengeProvisioner.provisionHttp(challenge.token(), keyAuth);
        }
        else {
            throw new IllegalArgumentException("unsupported challenge type " + challenge);
        }
    }

    private Challenge selectChallenge(Authorization authorization) {
        // HTTP challenges have priority
        if (authorization.hasChallenge("http-01")) {
            final var challenge = authorization.getChallenge("http-01");
            LOG.info("provisioning challenge {}", challenge);
            return challenge;
        }
        else if (authorization.hasChallenge("dns-01")) {
            final var challenge = authorization.getChallenge("dns-01");
            LOG.info("provisioning challenge {}", challenge);
            return challenge;
        }
        else {
            throw new IllegalArgumentException("cannot find any of the supported challenges (http-01, dns-01) in " +
                    authorization.challenges());
        }
    }

    private boolean isDnsProvisioningSupported() {
        return null != dnsChallengeProvisioner;
    }

    private boolean isHttpProvisioningSupported() {
        return null != httpChallengeProvisioner;
    }

}
