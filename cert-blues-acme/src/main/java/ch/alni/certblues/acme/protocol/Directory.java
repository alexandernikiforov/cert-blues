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

package ch.alni.certblues.acme.protocol;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import org.jetbrains.annotations.Nullable;

/**
 * In order to help clients configure themselves with the right URLs for each ACME operation, ACME servers provide a
 * directory object. This should be the only URL needed to configure clients. It is a JSON object, whose field names are
 * drawn from the resource registry and whose values are the corresponding URLs.
 */
@AutoValue
@JsonDeserialize(builder = Directory.Builder.class)
public abstract class Directory {

    public static Builder builder() {
        return new AutoValue_Directory.Builder();
    }

    @JsonGetter
    public abstract String newNonce();

    @JsonGetter
    public abstract String newAccount();

    @JsonGetter
    public abstract String newOrder();

    @JsonGetter
    @Nullable
    public abstract String newAuthz();

    @JsonGetter
    @Nullable
    public abstract String revokeCert();

    @JsonGetter
    @Nullable
    public abstract String keyChange();

    @JsonGetter
    @Nullable
    public abstract Meta meta();

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Builder {

        @JsonCreator
        static Builder create() {
            return builder();
        }

        @JsonSetter
        public abstract Builder newNonce(String value);

        @JsonSetter
        public abstract Builder newAccount(String value);

        @JsonSetter
        public abstract Builder newOrder(String value);

        @JsonSetter
        public abstract Builder newAuthz(String value);

        @JsonSetter
        public abstract Builder revokeCert(String value);

        @JsonSetter
        public abstract Builder keyChange(String value);

        @JsonSetter
        public abstract Builder meta(Meta value);

        public abstract Directory build();
    }
}
