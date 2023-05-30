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

package ch.alni.certblues.certbot;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.time.Instant;
import java.util.List;

/**
 * Information about current challenges.
 */
@AutoValue
public abstract class ChallengeInfo {

    public static Builder builder() {
        return new AutoValue_ChallengeInfo.Builder()
                .challengeUrls(List.of());
    }

    public abstract String certificateName();

    /**
     * The URL of the challenge.
     */
    public abstract ImmutableList<String> challengeUrls();

    /**
     * How long this challenge remains valid.
     */
    public abstract Instant expiresOn();

    /**
     * Form what time this challenge should be submitted.
     */
    public abstract Instant submitNotBefore();

    /**
     * The URL of the order these challenge is created for.
     */
    public abstract String orderUrl();

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder certificateName(String value);

        public abstract Builder expiresOn(Instant value);

        public abstract Builder submitNotBefore(Instant value);

        public abstract Builder challengeUrls(List<String> value);

        public abstract Builder orderUrl(String value);

        public abstract ChallengeInfo build();
    }
}
