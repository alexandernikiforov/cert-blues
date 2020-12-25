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

package ch.alni.certblues.acme.jws;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import org.jetbrains.annotations.Nullable;

/**
 * Content of the header. One of the kid or jwk must be present.
 */
@AutoValue
@JsonDeserialize(builder = JwsHeader.Builder.class)
public abstract class JwsHeader {

    public static Builder builder() {
        return new AutoValue_JwsHeader.Builder();
    }

    @JsonGetter
    public abstract String alg();

    @JsonGetter
    public abstract String nonce();

    @JsonGetter
    public abstract String url();

    @JsonGetter
    @Nullable
    public abstract String kid();

    @JsonGetter
    @Nullable
    public abstract PublicJwk jwk();

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {

        @JsonCreator
        static Builder create() {
            return builder();
        }

        @JsonSetter
        public abstract Builder alg(String value);

        @JsonSetter
        public abstract Builder nonce(String value);

        @JsonSetter
        public abstract Builder url(String value);

        @JsonSetter
        public abstract Builder kid(String value);

        @JsonSetter
        public abstract Builder jwk(PublicJwk value);

        abstract JwsHeader autoBuild();

        public JwsHeader build() {
            final var header = autoBuild();
            Preconditions.checkState(header.jwk() != null || header.kid() != null,
                    "Either kid or jwk must be present");
            return header;
        }
    }
}
