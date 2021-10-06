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

package ch.alni.certblues.acme.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Static utility class to work with JSON objects
 */
public class JsonObjects {
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getObjectMapper();

    /**
     * Deserializes the given string into Java object.
     *
     * @param value JSON as string
     * @param clazz the class of the Java object
     * @param <T>   class parameter
     * @return the deserialized object or throws an exception
     * @throws JsonObjectException if the given string cannot be de-serialized into the object of the provided class
     */
    public static <T> T deserialize(String value, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readerFor(clazz).readValue(value);
        }
        catch (JsonProcessingException e) {
            throw new JsonObjectException("error while parsing the JSON: " + value, e);
        }
    }

    /**
     * Serializes the given object into JSON as string.
     *
     * @param value object to serialize
     * @return the JSON representation as string
     * @throws JsonObjectException if the object cannot be serialized into JSON
     */
    public static String serialize(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        }
        catch (JsonProcessingException e) {
            throw new JsonObjectException("error while serializing JSON object: " + value, e);
        }
    }
}
