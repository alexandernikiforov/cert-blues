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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;

/**
 * An ACME challenge object represents a server's offer to validate a client's possession of an identifier in a specific
 * way.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = HttpChallenge.class, name = "http-01"),
        @JsonSubTypes.Type(value = DnsChallenge.class, name = "dns-01"),
        @JsonSubTypes.Type(value = TlsAplnChallenge.class, name = "tls-alpn-01")
})
public interface Challenge {

    @JsonGetter
    @JsonTypeId
    String type();

    @JsonGetter
    ChallengeStatus status();

    @JsonGetter
    String url();

    @JsonGetter
    String token();

    @JsonGetter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Nullable OffsetDateTime validated();

    @JsonGetter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Nullable Error error();

    interface Builder<BuilderType> {

        @JsonSetter
        BuilderType status(ChallengeStatus value);

        @JsonSetter
        BuilderType type(String value);

        @JsonSetter
        BuilderType url(String value);

        @JsonSetter
        BuilderType token(String value);

        @JsonSetter
        BuilderType validated(OffsetDateTime value);

        @JsonSetter
        BuilderType error(Error value);
    }
}
