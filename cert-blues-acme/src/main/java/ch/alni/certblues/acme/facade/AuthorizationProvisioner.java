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

import ch.alni.certblues.acme.key.Thumbprints;
import ch.alni.certblues.acme.protocol.*;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles authorization by preparing the key authorization and provisioning one of the challenges.
 */
class AuthorizationProvisioner {
    private static final Logger LOG = getLogger(AuthorizationProvisioner.class);

    private final Mono<String> publicKeyThumbprintMono;

    AuthorizationProvisioner(Mono<String> publicKeyThumbprintMono) {
        this.publicKeyThumbprintMono = publicKeyThumbprintMono;
    }

    /**
     * Calls the process of the identifier authorization. Returns a mono that emits when the client has handled this
     * authorization.
     *
     * @param authorization the authorization to handle
     * @return the mono of the challenge that has been provisioned
     */
    Mono<Challenge> process(Authorization authorization, AuthorizationProvisioningStrategy strategy) {
        switch (authorization.status()) {
            case EXPIRED:
            case DEACTIVATED:
            case INVALID:
            case REVOKED:
                return Mono.error(new IllegalArgumentException("cannot proceed with authorization of status " + authorization.status()));
            case VALID:
                // this authorization is already done, there should be at least one valid challenge
                final var submittedChallenge = selectValidChallenge(authorization);
                return Mono.just(submittedChallenge);
            case PENDING:
                final var challenge = selectChallenge(authorization, strategy);
                if (challenge.status() != ChallengeStatus.PENDING) {
                    // do not provision challenges that are not pending
                    return Mono.just(challenge);
                }

                // calculate the key auth
                final Mono<String> keyAuthMono = publicKeyThumbprintMono.map(publicKeyThumbprint -> challenge.token() + "." + publicKeyThumbprint);

                // then provision and return the provisioned challenge
                return keyAuthMono.flatMap(keyAuth -> provision(authorization.identifier(), challenge, keyAuth, strategy)).then(Mono.just(challenge));
            default:
                throw new IllegalArgumentException("unsupported authorization status " + authorization.status());
        }
    }

    private Mono<Void> provision(Identifier identifier, Challenge challenge, String keyAuth, AuthorizationProvisioningStrategy strategy) {
        if (challenge instanceof DnsChallenge && strategy.isDnsProvisioningSupported()) {
            final var name = identifier.value();
            final var value = Thumbprints.getSha256Digest(keyAuth);
            return strategy.getDnsChallengeProvisioner().provisionDns(name, value);
        }
        else if (challenge instanceof HttpChallenge && strategy.isHttpProvisioningSupported()) {
            return strategy.getHttpChallengeProvisioner().provisionHttp(challenge.token(), keyAuth);
        }
        else {
            throw new IllegalArgumentException("unsupported challenge type " + challenge);
        }
    }

    /**
     * Selects the first valid challenge.
     *
     * @param authorization authorization that is valid
     */
    private Challenge selectValidChallenge(Authorization authorization) {
        return authorization.challenges().stream()
                .filter(challenge -> challenge.status() == ChallengeStatus.VALID)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no valid challenges found for authorization " + authorization));
    }

    private Challenge selectChallenge(Authorization authorization, AuthorizationProvisioningStrategy strategy) {
        // HTTP challenges have priority
        if (authorization.hasChallenge("http-01") && strategy.isHttpProvisioningSupported()) {
            final var challenge = authorization.getChallenge("http-01");
            LOG.info("provisioning HTTP challenge {}", challenge);
            return challenge;
        } else if (authorization.hasChallenge("dns-01") && strategy.isDnsProvisioningSupported()) {
            final var challenge = authorization.getChallenge("dns-01");
            LOG.info("provisioning DNS challenge {}", challenge);
            return challenge;
        }
        else {
            throw new IllegalArgumentException("cannot find any of the supported challenges (http-01, dns-01) in " + authorization.challenges());
        }
    }
}
