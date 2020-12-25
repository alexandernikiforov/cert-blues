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

package ch.alni.certblues.acme.client.impl;

import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import ch.alni.certblues.acme.client.AcmeServerException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Creates a handle over another handle to retry the calls with bad nonce responses.
 */
final class RetryableHandle {
    private static final Logger LOG = getLogger(RetryableHandle.class);

    private RetryableHandle() {
    }

    static <T> T create(Session session, T object, Class<T> clazz) {
        return clazz.cast(Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz},
                (proxy, method, args) -> {
                    int retryCount = 10;
                    while (true) {
                        try {
                            return method.invoke(object, args);
                        }
                        catch (InvocationTargetException e) {
                            final Throwable cause = e.getCause();
                            if (cause instanceof AcmeServerException) {
                                final AcmeServerException ase = (AcmeServerException) cause;
                                if (retryCount > 0 && ase.isRetryable()) {
                                    LOG.info("the request request ended with an exception but will be retried: {}",
                                            ase.getError());
                                    retryCount -= 1;
                                    session.updateNonce();
                                    continue;
                                }
                            }

                            // unpack since it is not allowed to throw a checked exception that is undeclared
                            // on the method, and InvocationTargetException would be such an unchecked exception
                            throw cause;
                        }
                    }
                })
        );
    }
}
