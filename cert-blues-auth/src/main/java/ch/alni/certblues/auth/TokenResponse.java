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

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/**
 * Token issued by Azure Active Directory.
 */
@AutoValue
@JsonDeserialize(builder = TokenResponse.Builder.class)
public abstract class TokenResponse {

    private static final long EXPIRES_ON_DEFAULT = -1L;

    public static Builder builder() {
        return new AutoValue_TokenResponse.Builder().expiresOn(EXPIRES_ON_DEFAULT);
    }

    @JsonGetter("token_type")
    public abstract String tokenType();

    @JsonGetter("access_token")
    public abstract String accessToken();

    @JsonGetter("refresh_token")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Nullable
    public abstract String refreshToken();

    @JsonGetter("expires_in")
    public abstract long expiresIn();

    @JsonGetter("expires_on")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract long expiresOn();

    @JsonGetter("not_before")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Nullable
    public abstract Long notBefore();

    @JsonGetter("resource")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Nullable
    public abstract String resource();

    public abstract Builder toBuilder();

    @JsonIgnore
    public boolean isExpired() {
        // set the expiration buffer to 2 minutes
        return Instant.now().isAfter(Instant.ofEpochSecond(expiresOn()).minusSeconds(120));
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {

        @JsonCreator
        static Builder create() {
            return builder();
        }

        @JsonSetter("token_type")
        public abstract Builder tokenType(String value);

        @JsonSetter("access_token")
        public abstract Builder accessToken(String value);

        @JsonSetter("refresh_token")
        public abstract Builder refreshToken(String value);

        @JsonSetter("expires_in")
        public abstract Builder expiresIn(long value);

        @JsonSetter("expires_on")
        public abstract Builder expiresOn(long value);

        @JsonSetter("not_before")
        public abstract Builder notBefore(Long value);

        @JsonSetter("resource")
        public abstract Builder resource(String value);

        abstract long expiresOn();

        abstract long expiresIn();

        abstract TokenResponse autoBuild();

        public TokenResponse build() {
            if (expiresOn() == EXPIRES_ON_DEFAULT) {
                // if not expires_on is not set, set it to now + seconds(expires_in)
                expiresOn(Instant.now().plusSeconds(expiresIn()).getEpochSecond());
            }
            return autoBuild();
        }
    }
}
