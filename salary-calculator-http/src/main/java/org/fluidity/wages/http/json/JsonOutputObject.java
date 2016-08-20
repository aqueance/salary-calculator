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

final class JsonOutputObject extends JsonOutputContainer implements JsonOutput.Object.Root {

    /**
     * Creates a new instance.
     *
     * @param parent the parent container, if any.
     * @param buffer the character buffer to accumulate JSON content.
     */
    JsonOutputObject(final JsonOutputContainer parent, final CharacterBuffer buffer) {
        super(parent, buffer, '{', '}');
    }

    @Override
    public void add(final String name, final String value) {
        appended();

        _prologue(name);

        buffer.direct('"');
        buffer.encode(value);
        buffer.direct('"');
    }

    @Override
    public void add(final String name, final long value) {
        _set(name, buffer.number(value));
    }

    @Override
    public void add(final String name, final double value) {
        _set(name, buffer.number(value));
    }

    @Override
    public void add(final String name, final boolean value) {
        _set(name, buffer.logical(value));
    }

    @Override
    public void missing(final String name) {
        appended();

        _prologue(name);
        buffer.encode("null");
    }

    @Override
    public JsonOutput.Object object(final String name) {
        appended();

        _prologue(name);

        return _object();
    }

    @Override
    public JsonOutput.Array array(final String name) {
        appended();

        _prologue(name);

        return _array();
    }

    /**
     * Emits the name of a property.
     *
     * @param name the name of the property whose JSON representation is being emitted.
     */
    private void _prologue(final String name) {
        buffer.direct('"');
        buffer.encode(name);
        buffer.direct('"');
        buffer.direct(':');
    }

    /**
     * Sets the named property to the given non-character value in this container.
     *
     * @param name  the name of the property
     * @param value the value of the property.
     */
    private void _set(final String name, final String value) {
        appended();

        _prologue(name);
        buffer.encode(value);
    }
}
