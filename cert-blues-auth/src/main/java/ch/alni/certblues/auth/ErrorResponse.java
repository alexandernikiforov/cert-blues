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
import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Error response if the token cannot be issued.
 */
@AutoValue
@JsonDeserialize(builder = ErrorResponse.Builder.class)
public abstract class ErrorResponse {

    public static Builder builder() {
        return new AutoValue_ErrorResponse.Builder()
                .errorCodes(List.of());
    }

    @JsonGetter
    public abstract String error();

    @JsonGetter("error_description")
    public abstract String errorDescription();

    @JsonGetter("error_codes")
    @Nullable
    public abstract ImmutableList<Long> errorCodes();

    @JsonGetter("timestamp")
    @Nullable
    public abstract OffsetDateTime timestamp();

    @JsonGetter("trace_id")
    @Nullable
    public abstract String traceId();

    @JsonGetter("correlation_id")
    @Nullable
    public abstract String correlationId();

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {

        @JsonCreator
        static Builder create() {
            return builder();
        }

        @JsonSetter("error")
        public abstract Builder error(String value);

        @JsonSetter("error_description")
        public abstract Builder errorDescription(String value);

        @JsonSetter("error_codes")
        public abstract Builder errorCodes(List<Long> value);

        @JsonSetter("timestamp")
        public abstract Builder timestamp(OffsetDateTime value);

        @JsonSetter("trace_id")
        public abstract Builder traceId(String value);

        @JsonSetter("correlation_id")
        public abstract Builder correlationId(String value);

        public abstract ErrorResponse build();
    }
}
