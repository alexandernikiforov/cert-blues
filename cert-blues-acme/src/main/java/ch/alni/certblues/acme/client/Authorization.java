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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * An ACME authorization object represents a server's authorization for an account to represent an identifier.  In
 * addition to the identifier, an authorization includes several metadata fields, such as the status of the
 * authorization (e.g., "pending", "valid", or "revoked") and which challenges were used to validate possession of the
 * identifier.
 */
@AutoValue
@JsonDeserialize(builder = Authorization.Builder.class)
public abstract class Authorization {

    public static Builder builder() {
        return new AutoValue_Authorization.Builder();
    }

    @JsonGetter
    public abstract Identifier identifier();

    @JsonGetter
    public abstract AuthorizationStatus status();

    @JsonGetter
    @Nullable
    public abstract OffsetDateTime expires();

    @JsonGetter
    public abstract ImmutableList<Challenge> challenges();

    @JsonGetter
    @Nullable
    public abstract Boolean wildcard();

    @JsonIgnore
    public boolean hasChallenge(String type) {
        return challenges().stream()
                .anyMatch(value -> value.type().equals(type));
    }

    @JsonIgnore
    public Challenge getChallenge(String type) {
        return challenges().stream()
                .filter(value -> value.type().equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("cannot find the challenge of type " + type));
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {

        @JsonCreator
        static Builder create() {
            return builder();
        }

        @JsonSetter
        public abstract Builder identifier(Identifier value);

        @JsonSetter
        public abstract Builder status(AuthorizationStatus value);

        @JsonSetter
        public abstract Builder expires(OffsetDateTime value);

        @JsonSetter
        public abstract Builder challenges(List<Challenge> value);

        @JsonSetter
        public abstract Builder wildcard(Boolean value);

        public abstract Authorization build();
    }
}
