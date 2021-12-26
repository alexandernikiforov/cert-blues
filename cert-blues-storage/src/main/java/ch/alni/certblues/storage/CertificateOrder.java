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

package ch.alni.certblues.storage;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;

import ch.alni.certblues.common.json.ObjectMapperFactory;

/**
 * The original certificate order.
 */
@AutoValue
@JsonDeserialize(builder = CertificateOrder.Builder.class)
public abstract class CertificateOrder {

    public static Builder builder() {
        return new AutoValue_CertificateOrder.Builder();
    }

    /**
     * Transforms the given JSON string into a CertificateOrder object.
     *
     * @param json JSON string representing CertificateOrder
     * @return CertificateOrder object
     * @throws IllegalArgumentException if the object creation fails
     */
    public static CertificateOrder of(String json) {
        try {
            return ObjectMapperFactory.getObjectMapper().readerFor(CertificateOrder.class).readValue(json);
        }
        catch (JsonProcessingException e) {
            throw new IllegalArgumentException("cannot create CertificateOrder object from the provided string", e);
        }
    }

    @JsonGetter
    public abstract int keySize();

    @JsonGetter
    public abstract KeyType keyType();

    @JsonGetter
    public abstract int validityInMonths();

    @JsonGetter
    public abstract ImmutableList<String> dnsNames();

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public static abstract class Builder {

        @JsonCreator
        static Builder create() {
            return builder();
        }

        @JsonSetter
        public abstract Builder keySize(int value);

        @JsonSetter
        public abstract Builder keyType(KeyType value);

        @JsonSetter
        public abstract Builder validityInMonths(int value);

        @JsonSetter
        public abstract Builder dnsNames(List<String> value);

        public abstract CertificateOrder build();
    }
}
