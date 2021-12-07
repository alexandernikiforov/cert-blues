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

package ch.alni.certblues.acme.facade;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;

import ch.alni.certblues.acme.client.Challenge;
import ch.alni.certblues.acme.client.CreatedResource;
import ch.alni.certblues.acme.client.Order;

/**
 * Wrapper over provisioned order and challenges.
 */
@AutoValue
public abstract class ProvisionedOrder {

    public static ProvisionedOrder create(CreatedResource<Order> orderResource, List<Challenge> challenges) {
        return new AutoValue_ProvisionedOrder(orderResource, ImmutableList.copyOf(challenges));
    }

    /**
     * Contains the created order resource.
     */
    public abstract CreatedResource<Order> orderResource();

    /**
     * Returns provisioned challenges.
     */
    public abstract ImmutableList<Challenge> challenges();

}
