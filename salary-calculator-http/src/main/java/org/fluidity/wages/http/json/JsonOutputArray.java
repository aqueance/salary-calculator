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

final class JsonOutputArray extends JsonOutputContainer implements JsonOutput.Array.Root {

    /**
     * Creates a new instance.
     *
     * @param parent the parent container, if any.
     * @param buffer the character buffer to accumulate JSON content.
     */
    JsonOutputArray(final JsonOutputContainer parent, final CharacterBuffer buffer) {
        super(parent, buffer, '[', ']');
    }

    @Override
    public void add(final String value) {
        appended();

        buffer.direct('"');
        buffer.encode(value);
        buffer.direct('"');
    }

    /**
     * Adds a new given value to this container.
     *
     * @param value the value of the property.
     */
    private void _add(final String value) {
        appended();

        buffer.encode(value);
    }

    @Override
    public void add(final long value) {
        _add(buffer.number(value));
    }

    @Override
    public void add(final double value) {
        _add(buffer.number(value));
    }

    @Override
    public void add(final boolean value) {
        _add(buffer.logical(value));
    }

    @Override
    public void missing(final String name) {
        appended();

        buffer.encode("null");
    }

    @Override
    public JsonOutput.Object object() {
        appended();

        return _object();
    }

    @Override
    public JsonOutput.Array array() {
        appended();

        return _array();
    }
}
