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

import org.slf4j.Logger;

import ch.alni.certblues.acme.client.Authorization;
import ch.alni.certblues.acme.client.Challenge;
import ch.alni.certblues.acme.client.ChallengeProvisioner;
import ch.alni.certblues.acme.client.DnsChallenge;
import ch.alni.certblues.acme.client.HttpChallenge;
import ch.alni.certblues.acme.client.Identifier;
import ch.alni.certblues.acme.key.AccountKeyPair;
import ch.alni.certblues.acme.key.Thumbprints;
import reactor.core.publisher.Mono;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles authorization by preparing the key authorization and provisioning one of the challenges.
 */
public class AuthorizationHandler {
    private static final Logger LOG = getLogger(AuthorizationHandler.class);

    private final AccountKeyPair keyPair;
    private final ChallengeProvisioner provisioner;

    public AuthorizationHandler(AccountKeyPair keyPair, ChallengeProvisioner provisioner) {
        this.keyPair = keyPair;
        this.provisioner = provisioner;
    }

    /**
     * Returns a mono that emits when the client has handled this authorization.
     *
     * @param authorization the authorization to handle
     * @return the mono of the challenge that has been provisioned
     */
    public Mono<Challenge> handle(Authorization authorization) {
        switch (authorization.status()) {
            case EXPIRED:
            case DEACTIVATED:
            case INVALID:
            case REVOKED:
                return Mono.error(new IllegalArgumentException("cannot proceed with authorization of status " +
                        authorization.status()));
            case VALID:
                // this authorization is already done
                return Mono.empty();
            case PENDING:
                final var challenge = selectChallenge(authorization);
                return keyPair.getPublicKeyThumbprint()
                        .map(thumbprint -> challenge.token() + "." + thumbprint)
                        .flatMap(keyAuth -> provision(authorization.identifier(), challenge, keyAuth))
                        .then(Mono.just(challenge));
            default:
                throw new IllegalArgumentException("unsupported authorization status " + authorization.status());
        }
    }

    private Mono<Void> provision(Identifier identifier, Challenge challenge, String keyAuth) {
        if (challenge instanceof HttpChallenge) {
            return provisioner.provisionHttp(challenge.token(), keyAuth);
        }
        else if (challenge instanceof DnsChallenge) {
            final var name = "_acme-challenge." + identifier.value() + ".";
            final var value = Thumbprints.getSha256Digest(keyAuth);
            return provisioner.provisionDns(name, value);
        }
        else {
            throw new IllegalArgumentException("unsupported challenge type " + challenge);
        }
    }

    private Challenge selectChallenge(Authorization authorization) {
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
}
