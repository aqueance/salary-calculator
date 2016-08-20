package org.fluidity.wages.http.json;

final class JsonOutputArray extends JsonOutputContainer implements JsonOutput.Array {

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
