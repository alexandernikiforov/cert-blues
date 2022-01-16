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

package ch.alni.certblues.storage.certbot;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import ch.alni.certblues.common.json.ObjectMapperFactory;
import ch.alni.certblues.storage.JsonTransform;
import ch.alni.certblues.storage.KeyType;

/**
 * Request information to create/renew certificate.
 */
@AutoValue
@JsonDeserialize(builder = CertificateRequest.Builder.class)
public abstract class CertificateRequest implements JsonTransform {

    private static final int DEFAULT_KEY_SIZE = 2048;
    private static final KeyType DEFAULT_KEY_TYPE = KeyType.RSA;
    private static final int DEFAULT_VALIDITY_IN_MONTHS = 3;

    public static Builder builder() {
        return new AutoValue_CertificateRequest.Builder()
                .keySize(DEFAULT_KEY_SIZE)
                .keyType(DEFAULT_KEY_TYPE)
                .validityInMonths(DEFAULT_VALIDITY_IN_MONTHS);
    }

    /**
     * Transforms the given JSON string into a CertificateRequest object.
     *
     * @param json JSON string representing CertificateRequest
     * @return CertificateRequest object
     * @throws IllegalArgumentException if the object creation fails
     */
    public static CertificateRequest of(String json) {
        try {
            return ObjectMapperFactory.getObjectMapper().readerFor(CertificateRequest.class).readValue(json);
        }
        catch (JsonProcessingException e) {
            throw new IllegalArgumentException("cannot create CertificateRequest object from the provided string", e);
        }
    }

    /**
     * The size of the key to use. Default is 2048.
     */
    @JsonGetter
    public abstract int keySize();

    /**
     * The type of the key. Default is RSA.
     */
    @JsonGetter
    public abstract KeyType keyType();

    /**
     * Validity of the future certificate in months. Default is 3.
     */
    @JsonGetter
    public abstract int validityInMonths();

    /**
     * Subject name for the certificate. Must include a domain name.
     */
    @JsonGetter
    public abstract String subjectDn();

    /**
     * A list of DNS names to be supported by the new certificate.
     */
    @JsonGetter
    public abstract ImmutableList<String> dnsNames();

    /**
     * Unique name to be given to this certificate.
     */
    @JsonGetter
    public abstract String certificateName();

    /**
     * URL of the storage endpoint. It points to the blob container that holds the server's WEB content and thus used
     * for HTTP challenges. The storage endpoint includes the SAS token so that the function app can access it.
     */
    @JsonGetter
    public abstract String storageEndpointUrl();

    /**
     * Resource group of the DNS zone to be used in the DNS challenges. It can be null if DNS challenges are not used.
     */
    @JsonGetter
    @Nullable
    public abstract String dnsZoneResourceGroup();

    /**
     * DNS zone name to be used in the DNS challenges. This is the resource name within the resource group. It can be
     * null if DNS challenges are not used.
     */
    @JsonGetter
    @Nullable
    public abstract String dnsZone();

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

        @JsonSetter
        public abstract Builder subjectDn(String value);

        @JsonSetter
        public abstract Builder certificateName(String value);

        @JsonSetter
        public abstract Builder storageEndpointUrl(String value);

        @JsonSetter
        public abstract Builder dnsZoneResourceGroup(String value);

        @JsonSetter
        public abstract Builder dnsZone(String value);

        public abstract CertificateRequest build();
    }
}
