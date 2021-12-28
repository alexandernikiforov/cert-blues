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

package ch.alni.certblues.functions;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.QueueOutput;
import com.microsoft.azure.functions.annotation.TimerTrigger;

import java.util.Optional;

import ch.alni.certblues.azure.queue.AzureQueue;
import ch.alni.certblues.storage.queue.Queue;

/**
 * First function to test.
 */
@SuppressWarnings("unused")
public class Function {

    // static initialization makes sure that context is initialized only once at the start of the instance
    private static final Context CONTEXT = Context.getInstance();

    @FunctionName("sample")
    public HttpResponseMessage getResponse(
            @HttpTrigger(name = "request", authLevel = AuthorizationLevel.ANONYMOUS, methods = {HttpMethod.GET})
                    HttpRequestMessage<Optional<String>> request,
            @QueueOutput(
                    connection = "requestQueueConnection",
                    name = "requestQueue",
                    queueName = "requests") OutputBinding<String> content,
            final ExecutionContext context) {

        context.getLogger().info("Java HTTP function executed at: " + java.time.LocalDateTime.now());

        final TokenCredential credential = CONTEXT.getCredential();
        final Configuration configuration = CONTEXT.getConfiguration();
        final HttpClient httpClient = CONTEXT.getHttpClient();

        final Queue storageService = new AzureQueue(
                credential, httpClient, configuration.queueServiceUrl(), configuration.requestQueueName()
        );

        // Parse name parameter
        final String name = request.getQueryParameters().get("name");

        if (name == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a name on the name string or in the request body")
                    .build();
        }
        else {
            storageService.put("Hey, " + name).block();

            final String value = "Hello, " + name;
            content.setValue(value);
            return request.createResponseBuilder(HttpStatus.OK).body(value).build();
        }
    }

    @FunctionName("trigger")
    public void timedCall(
            @TimerTrigger(name = "keepAlive", schedule = "%Schedule%")
                    String timerInfo,
            final ExecutionContext context) {

        context.getLogger().info("Timer is triggered: " + timerInfo);
    }
}
