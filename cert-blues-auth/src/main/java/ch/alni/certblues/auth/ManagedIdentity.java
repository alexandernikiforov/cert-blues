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

import org.jetbrains.annotations.Nullable;

/**
 * Managed identity parameters.
 */
@AutoValue
public abstract class ManagedIdentity {

    public static Builder builder() {
        return new AutoValue_ManagedIdentity.Builder();
    }

    public abstract String getResource();

    @Nullable
    public abstract String getObjectId();

    @Nullable
    public abstract String getClientId();

    @Nullable
    public abstract String getManagedIdentityResourceId();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setResource(String value);

        public abstract Builder setObjectId(String value);

        public abstract Builder setClientId(String value);

        public abstract Builder setManagedIdentityResourceId(String value);

        public abstract ManagedIdentity build();
    }
}
