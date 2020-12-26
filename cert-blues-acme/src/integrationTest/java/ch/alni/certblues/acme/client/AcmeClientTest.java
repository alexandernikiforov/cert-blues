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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import ch.alni.certblues.acme.client.impl.AcmeClientBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class AcmeClientTest {
    private static final Logger LOG = getLogger(AcmeClientTest.class);

    private static final String DIRECTORY_URL = "https://acme-staging-v02.api.letsencrypt.org/directory";

    private final AcmeClient client = new AcmeClientBuilder()
            .build();

    @Test
    void getAccount() throws Exception {
        final DirectoryHandle directoryHandle = client.getDirectory(DIRECTORY_URL);

        final Directory directory = directoryHandle.getDirectory();
        assertThat(directory.meta()).isNotNull();
        assertThat(directory.meta().caaIdentities()).contains("letsencrypt.org");

        LOG.debug("directory: {}", directory);
    }
}
