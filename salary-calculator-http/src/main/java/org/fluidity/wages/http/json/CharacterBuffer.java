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

/**
 * A character buffer that converts Unicode code points to JSON formatted characters.
 */
@SuppressWarnings("WeakerAccess")
final class CharacterBuffer {

    private final Consumer<String> consumer;

    private final char[] buffer;
    private int index = 0;

    public CharacterBuffer(final int size, final Consumer<String> consumer) {
        this.buffer = new char[size];
        this.consumer = consumer;
    }

    /**
     * Encodes the given character string to JSON format. See http://json.org/
     *
     * @param text the character string to encode.
     */
    public void encode(final String text) {
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

                break;
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

    /**
     * Emits the given character after a slash character.
     *
     * @param character the character to emit.
     */
    private void escape(final char character) {
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
    private void control(final int codePoint) {
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
}
