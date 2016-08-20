package org.fluidity.wages.http.json;

final class JsonOutputObject extends JsonOutputContainer implements JsonOutput.Object {

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
