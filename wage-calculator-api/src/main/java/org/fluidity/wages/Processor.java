package org.fluidity.wages;

import java.util.function.Consumer;

/**
 * Consumes and processes a stream of objects. The two extended interfaces together allow using the processor in a try-with-resource construct, like so:
 * <pre>
 *     try (final Processor&lt;&hellip;&gt; processor = &hellip;) {
 *         processor.accept(&hellip;)
 *     }
 * </pre>
 *
 * The extra {@link #flush()} method allows demarcation of the input stream.
 */
public interface Processor<T> extends Consumer<T>, AutoCloseable {

    /**
     * Flushes the details computed so far to the downstream {@link Consumer} or {@link Processor}.
     */
    void flush();

    /**
     * Closes this processor. The instance will not be usable afterwards.
     */
    @Override
    void close();
}
