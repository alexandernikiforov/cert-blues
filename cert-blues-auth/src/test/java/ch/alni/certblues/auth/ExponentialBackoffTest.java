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

package ch.alni.certblues.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExponentialBackoffTest {

    @Test
    void getNextAttemptDelay() {
        final var strategy = new ExponentialBackoff.Builder()
                .minBackoff(Duration.ZERO)
                .maxBackoff(Duration.ofSeconds(60))
                .deltaBackoff(Duration.ofSeconds(2))
                .retryCount(5)
                .fastFirstRetry(false)
                .build();

        assertThat(strategy.hasNextAttempt()).isTrue();
        assertThat(strategy.getNextAttemptDelay()).isEqualTo(Duration.ofSeconds(0));

        assertThat(strategy.hasNextAttempt()).isTrue();
        assertThat(strategy.getNextAttemptDelay()).isEqualTo(Duration.ofSeconds(2));

        assertThat(strategy.hasNextAttempt()).isTrue();
        assertThat(strategy.getNextAttemptDelay()).isEqualTo(Duration.ofSeconds(6));

        assertThat(strategy.hasNextAttempt()).isTrue();
        assertThat(strategy.getNextAttemptDelay()).isEqualTo(Duration.ofSeconds(14));

        assertThat(strategy.hasNextAttempt()).isTrue();
        assertThat(strategy.getNextAttemptDelay()).isEqualTo(Duration.ofSeconds(30));

        assertThat(strategy.hasNextAttempt()).isFalse();
        assertThrows(IllegalStateException.class, strategy::getNextAttemptDelay);
    }

    @Test
    void getNextAttemptDelayWithMinBackoff() {
        final var strategy = new ExponentialBackoff.Builder()
                .minBackoff(Duration.ofSeconds(2))
                .maxBackoff(Duration.ofSeconds(60))
                .deltaBackoff(Duration.ofSeconds(2))
                .retryCount(5)
                .fastFirstRetry(false)
                .build();

        assertThat(strategy.hasNextAttempt()).isTrue();
        assertThat(strategy.getNextAttemptDelay()).isEqualTo(Duration.ofSeconds(2));

        assertThat(strategy.hasNextAttempt()).isTrue();
        assertThat(strategy.getNextAttemptDelay()).isEqualTo(Duration.ofSeconds(6));
    }

    @Test
    void getNextAttemptDelayWithMinBackoffAndFastFirstRetry() {
        final var strategy = new ExponentialBackoff.Builder()
                .minBackoff(Duration.ofSeconds(2))
                .maxBackoff(Duration.ofSeconds(60))
                .deltaBackoff(Duration.ofSeconds(2))
                .retryCount(5)
                .fastFirstRetry(true)
                .build();

        assertThat(strategy.hasNextAttempt()).isTrue();
        assertThat(strategy.getNextAttemptDelay()).isEqualTo(Duration.ofSeconds(2));

        assertThat(strategy.hasNextAttempt()).isTrue();
        assertThat(strategy.getNextAttemptDelay()).isEqualTo(Duration.ofSeconds(2));

        assertThat(strategy.hasNextAttempt()).isTrue();
        assertThat(strategy.getNextAttemptDelay()).isEqualTo(Duration.ofSeconds(6));
    }

}
