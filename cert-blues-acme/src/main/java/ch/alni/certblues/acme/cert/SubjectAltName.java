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

package ch.alni.certblues.acme.cert;

import com.google.auto.value.AutoValue;

/**
 * Value object for subject alt names.
 */
@AutoValue
public abstract class SubjectAltName {

    public static final int TYPE_DNS = 2;
    public static final int TYPE_IP_ADDRESS = 7;

    public static SubjectAltName create(int type, String value) {
        return new AutoValue_SubjectAltName(type, value);
    }

    public static SubjectAltName dns(String value) {
        return new AutoValue_SubjectAltName(TYPE_DNS, value);
    }

    public static SubjectAltName ipAddress(String value) {
        return new AutoValue_SubjectAltName(TYPE_IP_ADDRESS, value);
    }

    /**
     * Type of the SAN.
     */
    public abstract int getType();

    /**
     * The SAN value.
     */
    public abstract String getValue();
}
