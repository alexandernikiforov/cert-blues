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

package ch.alni.certblues.acme.key;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * RSA public key.
 */
@AutoValue
@JsonDeserialize(builder = RsaPublicJwk.Builder.class)
@JsonSerialize(as = RsaPublicJwk.class)
public abstract class RsaPublicJwk implements PublicJwk {

    public static Builder builder() {
        return new AutoValue_RsaPublicJwk.Builder()
                .kty("RSA");
    }

    @JsonGetter
    public abstract String n();

    @JsonGetter
    public abstract String e();

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Builder implements PublicJwk.Builder<Builder> {

        @JsonCreator
        static Builder create() {
            return builder();
        }

        @JsonSetter
        public abstract Builder n(String value);

        @JsonSetter
        public abstract Builder e(String value);

        public abstract RsaPublicJwk build();
    }
}
