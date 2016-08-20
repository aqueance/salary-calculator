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

/**
 * A JSON container, which is an abstract super type for JSON object and JSON array.
 */
@SuppressWarnings("WeakerAccess")
abstract class JsonOutputContainer implements JsonOutput.Stream {

    protected final CharacterBuffer buffer;

    // The parent container, if any.
    private final JsonOutputContainer parent;
    private final char closeMark;

    // The last child container, if any, that has been opened but not yet closed.
    private JsonOutput.Stream openChild;

    // Set when there at least one value has been added to this container.
    private boolean compound;

    // Set when this contains is closed and no further addition is allowed thereto.
    private boolean closed;

    /**
     * Creates a new JSON container. The characters that open and close the container are specified here.
     *
     * @param parent    the parent container, if any; may be <code>null</code>.
     * @param buffer    the buffer size.
     * @param openMark  the character that opens this container.
     * @param closeMark the character that closes this container.
     */
    protected JsonOutputContainer(final JsonOutputContainer parent, final CharacterBuffer buffer, final char openMark, final char closeMark) {
        this.parent = parent;
        this.buffer = buffer;
        this.closeMark = closeMark;
        this.buffer.direct(openMark);
    }

    /**
     * The child container notifies its the parent that it has been closed.
     *
     * @param child the child container that has been closed.
     */
    protected final void closed(final JsonOutput.Stream child) {
        if (openChild == child) {
            openChild = null;
        }
    }

    /**
     * Notification that a new item is being added to this container. The previously opened child container, if any, will be automatically closed, and a comma
     * will also be output if this is not the first item added to this container.
     */
    protected final void appended() {
        if (closed) {
            throw new IllegalStateException("This container has been closed");
        }

        if (openChild != null) {
            openChild.close();
        }

        if (compound) {
            buffer.direct(',');
        }

        compound = true;
    }

    @Override
    public final void close(final Runnable callback) {
        if (closed) {
            throw new IllegalStateException("This container has been closed");
        }

        if (openChild != null) {
            openChild.close();
        }

        buffer.direct(closeMark);

        if (parent != null) {
            parent.closed(this);
        }

        if (parent == null) {
            buffer.flush();
        }

        closed = true;

        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public final void close() {
        close(null);
    }

    /**
     * Adds a new JSON object to this container as its child container.
     *
     * @return a new JSON object; never <code>null</code>.
     */
    protected final JsonOutputObject _object() {
        final JsonOutputObject object = new JsonOutputObject(this, buffer);
        openChild = object;
        return object;
    }

    /**
     * Adds a new JSON array to this container as its child container.
     *
     * @return a new JSON array; never <code>null</code>.
     */
    protected final JsonOutput.Array _array() {
        final JsonOutputArray array = new JsonOutputArray(this, buffer);
        openChild = array;
        return array;
    }
}
