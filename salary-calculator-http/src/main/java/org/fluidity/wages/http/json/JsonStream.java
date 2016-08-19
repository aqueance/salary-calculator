package org.fluidity.wages.http.json;

import java.util.function.Consumer;

/**
 * Buffering JSON stream writer. Use the {@link #object(int, Consumer)} or {@link #array(int, Consumer)} methods to get a top-level JSON container, and then use
 * their respective methods to set properties or add elements thereto.
 * <p>
 * *NOTE*: This implementation allows duplicate keys to be set in the same JSON object. The JSON specification allows duplicate keys so this is valid
 * behavior.
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public class JsonStream {

    /**
     * Creates a new JSON object.
     *
     * @param buffer   the buffer size.
     * @param consumer the consumer of at most buffer sized chunks.
     *
     * @return a new object; never <code>null</code>.
     */
    public static JsonStream.Object object(final int buffer, final Consumer<String> consumer) {
        return new Object(null, new CharacterBuffer(buffer, consumer));
    }

    /**
     * Creates a new JSON array.
     *
     * @param buffer   the buffer size.
     * @param consumer the consumer of at most buffer sized chunks.
     *
     * @return a new object; never <code>null</code>.
     */
    public static JsonStream.Array array(final int buffer, final Consumer<String> consumer) {
        return new Array(null, new CharacterBuffer(buffer, consumer));
    }

    protected final CharacterBuffer buffer;

    // The parent container, if any.
    private final JsonStream parent;
    private final char closeMark;

    // The last child container, if any, that has been opened but not yet closed.
    private JsonStream openChild;

    // Set when there at least one value has been added to this container.
    private boolean compound;

    /**
     * Creates a new JSON container. The characters that open and close the container are specified here.
     *
     * @param parent    the parent container, if any; may be <code>null</code>.
     * @param buffer    the buffer size.
     * @param openMark  the character that opens this container.
     * @param closeMark the character that closes this container.
     */
    protected JsonStream(final JsonStream parent, final CharacterBuffer buffer, final char openMark, final char closeMark) {
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
    protected final void closed(final JsonStream child) {
        if (openChild == child) {
            openChild = null;
        }
    }

    /**
     * Notification that a new item is being added to this container. The previously opened child container, if any, will be automatically closed, and a comma
     * will also be output if this is not the first item added to this container.
     */
    protected final void appended() {
        if (openChild != null) {
            openChild.close();
        }

        if (compound) {
            buffer.direct(',');
        }

        compound = true;
    }

    /**
     * Closes this container. The last open child container, if any, is closed, and then the closing character is emitted. If this parent has no container, any
     * buffered content accumulated so far is also flushed.
     *
     * @param callback the object to invoke once this container has been closed; can be <code>null</code>.
     */
    public final void close(final Runnable callback) {
        if (openChild != null) {
            openChild.close();
        }

        if (parent != null) {
            parent.closed(this);
        }

        buffer.direct(closeMark);

        if (parent == null) {
            buffer.flush();
        }

        if (callback != null) {
            callback.run();
        }
    }

    /**
     * Invokes {@link #close(Runnable)} with no callback.
     */
    public final void close() {
        close(null);
    }

    /**
     * Adds a new JSON object to this container as its child container.
     *
     * @return a new JSON object; never <code>null</code>.
     */
    protected final JsonStream.Object _object() {
        final Object object = new Object(this, buffer);
        openChild = object;
        return object;
    }

    /**
     * Adds a new JSON array to this container as its child container.
     *
     * @return a new JSON array; never <code>null</code>.
     */
    protected final JsonStream.Array _array() {
        final Array array = new Array(this, buffer);
        openChild = array;
        return array;
    }

    /**
     * A JSON object emitter.
     */
    public static final class Object extends JsonStream {

        /**
         * Creates a new instance.
         *
         * @param parent the parent container, if any.
         * @param buffer the character buffer to accumulate JSON content.
         */
        Object(final JsonStream parent, final CharacterBuffer buffer) {
            super(parent, buffer, '{', '}');
        }

        /**
         * Sets the named property to the given value in this container.
         *
         * @param name  the name of the property
         * @param value the value of the property.
         */
        public void add(final String name, final String value) {
            appended();

            _prologue(name);

            buffer.direct('"');
            buffer.text(value);
            buffer.direct('"');
        }

        /**
         * Sets the named property to the given value in this container.
         *
         * @param name  the name of the property
         * @param value the value of the property.
         */
        public void add(final String name, final long value) {
            _set(name, buffer.number(value));
        }

        /**
         * Sets the named property to the given value in this container.
         *
         * @param name  the name of the property
         * @param value the value of the property.
         */
        public void add(final String name, final double value) {
            _set(name, buffer.number(value));
        }

        /**
         * Sets the named property to the given value in this container.
         *
         * @param name  the name of the property
         * @param value the value of the property.
         */
        public void add(final String name, final boolean value) {
            _set(name, buffer.logical(value));
        }

        /**
         * Sets the named property to null in this container.
         *
         * @param name the name of the property
         */
        public void missing(final String name) {
            appended();

            _prologue(name);
            buffer.text("null");
        }

        /**
         * Sets the named property to a new JSON object in this container.
         *
         * @param name the name of the property
         */
        public JsonStream.Object object(final String name) {
            appended();

            _prologue(name);

            return _object();
        }

        /**
         * Sets the named property to a new JSON array in this container.
         *
         * @param name the name of the property
         */
        public JsonStream.Array array(final String name) {
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
            buffer.text(name);
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
            buffer.text(value);
        }
    }

    /**
     * A JSON array emitter.
     */
    public static final class Array extends JsonStream {

        /**
         * Creates a new instance.
         *
         * @param parent the parent container, if any.
         * @param buffer the character buffer to accumulate JSON content.
         */
        Array(final JsonStream parent, final CharacterBuffer buffer) {
            super(parent, buffer, '[', ']');
        }

        /**
         * Adds a new given value to this container.
         *
         * @param value the value of the property.
         */
        public void add(final String value) {
            appended();

            buffer.direct('"');
            buffer.text(value);
            buffer.direct('"');
        }

        /**
         * Adds a new given value to this container.
         *
         * @param value the value of the property.
         */
        public void _add(final String value) {
            appended();

            buffer.text(value);
        }

        /**
         * Adds a new given value to this container.
         *
         * @param value the value of the property.
         */
        public void add(final long value) {
            _add(buffer.number(value));
        }

        /**
         * Adds a new given value to this container.
         *
         * @param value the value of the property.
         */
        public void add(final double value) {
            _add(buffer.number(value));
        }

        /**
         * Adds a new given value to this container.
         *
         * @param value the value of the property.
         */
        public void add(final boolean value) {
            _add(buffer.logical(value));
        }

        /**
         * Sets the named property to null in this container.
         *
         * @param name the name of the property
         */
        public void missing(final String name) {
            appended();

            buffer.text("null");
        }

        /**
         * Adds a new JSON object to this container.
         */
        public JsonStream.Object object() {
            appended();

            return _object();
        }

        /**
         * Adds a new JSON array to this container.
         */
        public JsonStream.Array array() {
            appended();

            return _array();
        }
    }

    /**
     * A character buffer that converts Unicode code points to JSON formatted characters.
     */
    private static final class CharacterBuffer {

        private final char[] buffer;

        private int index = 0;

        private final Consumer<String> consumer;

        private CharacterBuffer(final int size, final Consumer<String> consumer) {
            this.buffer = new char[size];
            this.consumer = consumer;
        }

        /**
         * Encodes the given character string to JSON format. See http://json.org/
         *
         * @param text the character string to encode.
         */
        public void text(final String text) {
            verify(text);

            for (int offset = 0, length = text.length(); offset < length; ++offset) {
                final int codePoint = text.codePointAt(offset);

                switch (codePoint) {
                case '\\':
                    escape('\\');
                    break;
                case '"':
                    escape('"');
                    break;
                case '\b':
                    escape('b');
                    break;
                case '\f':
                    escape('f');
                    break;
                case '\n':
                    escape('n');
                    break;
                case '\r':
                    escape('r');
                    break;
                case '\t':
                    escape('t');
                    break;
                default:
                    if (Character.isISOControl(codePoint)) {
                        control(codePoint);
                    } else {
                        direct((char) codePoint);
                    }
                }
            }
        }

        /**
         * Chokes on Unicode code points outside the Basic Multilingual Plane.
         *
         * @param text the character string to verify.
         *
         * @throws IllegalArgumentException when a non-BMP character is found.
         */
        private void verify(final String text) throws IllegalArgumentException {
            for (int offset = 0, length = text.length(); offset < length; ++offset) {
                if (Character.charCount(text.codePointAt(offset)) > 1) {
                    throw new IllegalArgumentException("JSON can only encode BMP characters");
                }
            }
        }

        /**
         * Emits the given character as is.
         *
         * @param character the character to emit.
         */
        public void direct(final char character) {
            if (index + 1 >= buffer.length) {
                flush();
            }

            buffer[index++] = character;
        }

        /**
         * Emits the given character after a slash character.
         *
         * @param character the character to emit.
         */
        public void escape(final char character) {
            if (index + 2 >= buffer.length) {
                flush();
            }

            buffer[index] = '\\';
            buffer[index + 1] = character;

            index += 2;
        }

        /**
         * Emits the given character after as a Unicode code point.
         *
         * @param codePoint the character to emit.
         */
        public void control(final int codePoint) {
            if (index + 6 >= buffer.length) {
                flush();
            }

            buffer[index] = '\\';
            buffer[index + 1] = 'u';

            final String hex = Integer.toHexString(codePoint);
            final int length = hex.length();

            buffer[index + 2] = digit(hex, length, 0, 4);
            buffer[index + 3] = digit(hex, length, 1, 4);
            buffer[index + 4] = digit(hex, length, 2, 4);
            buffer[index + 5] = digit(hex, length, 3, 4);

            index += 6;
        }

        /**
         * Pretends that the given number with the given length is padded to the given padded length, and returns the character at the given index.
         *
         * @param number the number to pad.
         * @param length the length to pad the number to.
         * @param index  the index of the digit to return.
         * @param padded the pretended padded length.
         *
         * @return the digit at the given index.
         */
        private char digit(final String number, final int length, final int index, final int padded) {
            final int offset = index + length - padded;
            return offset < 0 ? '0' : number.charAt(offset);
        }

        /**
         * Returns the text representation of the given integer number.
         *
         * @param number the integer value.
         *
         * @return a string; never <code>null</code>.
         */
        public String number(final long number) {
            return String.valueOf(number);
        }

        /**
         * Returns the text representation of the given floating point number. Infinity and NaN values are rejected, and integer values are encoded as such.
         *
         * @param number the floating point value.
         *
         * @return a string; never <code>null</code>.
         */
        public String number(final double number) {
            if (Double.isInfinite(number) || Double.isNaN(number)) {
                throw new IllegalArgumentException("JSON can only encode finite numbers");
            }

            final long integer = (long) number;
            return number == (double) integer ? String.valueOf(integer) : String.valueOf(number);
        }

        /**
         * Returns the text representation of the given boolean value.
         *
         * @param value the boolean value.
         *
         * @return a string; never <code>null</code>.
         */
        public String logical(final boolean value) {
            return String.valueOf(value);
        }

        /**
         * If the accumulated buffer is not empty, flushes its content to the consumer and empties the buffer.
         */
        public void flush() {
            if (index > 0) {
                consumer.accept(new String(buffer, 0, index));
                index = 0;
            }
        }
    }
}
