/*
 * Copyright (c) 2016 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.wages.http.json;

import java.util.function.Consumer;

import org.fluidity.foundation.Utility;

/**
 * Buffering JSON stream writer. Use the {@link #object(int, Consumer)} or {@link #array(int, Consumer)} methods to get a top-level JSON container, and then use
 * their respective methods to set properties or add elements thereto. When done, make sure to invoke {@link JsonOutput.Stream#close()} or {@link
 * JsonOutput.Stream#close(Runnable)} on the root object; those methods on intermediate objects or arrays are optional and might not do anything.
 * <p>
 * Intermediate containers, i.e., {@link JsonOutput.Object} and {@link JsonOutput.Array} instances, are automatically closed when a new property or item is
 * added, respectively, to their parent container.
 * <p>
 * *NOTE*: This implementation allows duplicate keys to be set in the same JSON object. The JSON specification allows duplicate keys, so that is valid
 * behavior.
 */
@SuppressWarnings("WeakerAccess")
public final class JsonOutput extends Utility {

    private JsonOutput() { }

    /**
     * Creates a new JSON object.
     *
     * @param buffer   the buffer size.
     * @param consumer the consumer of at most buffer sized chunks.
     *
     * @return a new object; never <code>null</code>.
     */
    public static Object.Root object(final int buffer, final Consumer<String> consumer) {
        return new JsonOutputObject(null, buffered(consumer, buffer));
    }

    /**
     * Creates a new JSON array.
     *
     * @param buffer   the buffer size.
     * @param consumer the consumer of at most buffer sized chunks.
     *
     * @return a new object; never <code>null</code>.
     */
    public static Array.Root array(final int buffer, final Consumer<String> consumer) {
        return new JsonOutputArray(null, buffered(consumer, buffer));
    }

    /**
     * Creates a new character buffer with the given size.
     *
     * @param consumer the consumer to flush the buffer to.
     * @param size     the size of the buffer.
     *
     * @return a new object; never <code>null</code>.
     */
    private static CharacterBuffer buffered(final Consumer<String> consumer, final int size) {
        return new CharacterBuffer(size, consumer);
    }

    /**
     * A JSON array emitter.
     */
    public interface Array {

        /**
         * Adds a new given value to this container.
         *
         * @param value the value of the property.
         */
        void add(String value);

        /**
         * Adds a new given value to this container.
         *
         * @param value the value of the property.
         */
        void add(long value);

        /**
         * Adds a new given value to this container.
         *
         * @param value the value of the property.
         */
        void add(double value);

        /**
         * Adds a new given value to this container.
         *
         * @param value the value of the property.
         */
        void add(boolean value);

        /**
         * Sets the named property to null in this container.
         *
         * @param name the name of the property
         */
        void missing(String name);

        /**
         * Adds a new JSON object to this container.
         */
        Object object();

        /**
         * Adds a new JSON array to this container.
         */
        Array array();

        /**
         * A JSON root array. When finished outputting JSON, one of the {@link Stream} methods must be called.
         */
        interface Root extends Array, Stream {}
    }

    /**
     * A JSON object emitter.
     */
    public interface Object {

        /**
         * Sets the named property to the given value in this container.
         *
         * @param name  the name of the property
         * @param value the value of the property.
         */
        void add(String name, String value);

        /**
         * Sets the named property to the given value in this container.
         *
         * @param name  the name of the property
         * @param value the value of the property.
         */
        void add(String name, long value);

        /**
         * Sets the named property to the given value in this container.
         *
         * @param name  the name of the property
         * @param value the value of the property.
         */
        void add(String name, double value);

        /**
         * Sets the named property to the given value in this container.
         *
         * @param name  the name of the property
         * @param value the value of the property.
         */
        void add(String name, boolean value);

        /**
         * Sets the named property to null in this container.
         *
         * @param name the name of the property
         */
        void missing(String name);

        /**
         * Sets the named property to a new JSON object in this container.
         *
         * @param name the name of the property
         */
        Object object(String name);

        /**
         * Sets the named property to a new JSON array in this container.
         *
         * @param name the name of the property
         */
        Array array(String name);

        /**
         * A JSON root object. When finished outputting JSON, one of the {@link Stream} methods must be called.
         */
        interface Root extends Object, Stream {}
    }

    /**
     * A closeable stream.
     */
    public interface Stream {

        /**
         * Closes this container. The last open child container, if any, is closed, and then the closing character is emitted. If this parent has no container, any
         * buffered content accumulated so far is also flushed.
         *
         * @param callback the object to invoke once this container has been closed; can be <code>null</code>.
         */
        void close(Runnable callback);

        /**
         * Invokes {@link #close(Runnable)} with no callback.
         */
        void close();
    }
}
