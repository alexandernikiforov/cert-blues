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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * An ACME order object represents a client's request for a certificate and is used to track the progress of that order
 * through to issuance. Thus, the object contains information about the requested certificate, the authorizations that
 * the server requires the client to complete, and any certificates that have resulted from this order.
 */
@AutoValue
@JsonDeserialize(builder = Order.Builder.class)
public abstract class Order {

    public static Builder builder() {
        return new AutoValue_Order.Builder();
    }

    @JsonGetter
    public abstract OrderStatus status();

    @JsonGetter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Nullable
    public abstract OffsetDateTime expires();

    @JsonGetter
    public abstract ImmutableList<Identifier> identifiers();

    @JsonGetter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Nullable
    public abstract OffsetDateTime notBefore();

    @JsonGetter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Nullable
    public abstract OffsetDateTime notAfter();

    @JsonGetter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Nullable
    public abstract JsonNode errors();

    @JsonGetter
    public abstract ImmutableList<String> authorizations();

    @JsonGetter("finalize")
    public abstract String finalizeUrl();

    @JsonGetter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Nullable
    public abstract String certificate();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {

        @JsonCreator
        static Builder create() {
            return builder();
        }

        @JsonSetter
        public abstract Builder status(OrderStatus value);

        @JsonSetter
        public abstract Builder expires(OffsetDateTime value);

        @JsonSetter
        public abstract Builder identifiers(List<Identifier> value);

        @JsonSetter
        public abstract Builder notBefore(OffsetDateTime value);

        @JsonSetter
        public abstract Builder notAfter(OffsetDateTime value);

        @JsonSetter
        public abstract Builder errors(JsonNode value);

        @JsonSetter
        public abstract Builder authorizations(List<String> value);

        @JsonSetter("finalize")
        public abstract Builder finalizeUrl(String value);

        @JsonSetter
        public abstract Builder certificate(String value);

        public abstract Order build();
    }
}
