package org.fluidity.wages;

import java.util.function.Consumer;

/**
 * Consumes and processes a stream of objects. The two extended interfaces together allow using the processor in a try-with-resource construct, like so:
 * <pre>
 *     try (final BatchProcessor&lt;&hellip;&gt; processor = &hellip;) {
 *         processor.accept(&hellip;)
 *     }
 * </pre>
 *
 * @param <T> the type of the input to the processor.
 */
public interface BatchProcessor<T> extends Consumer<T>, AutoCloseable {

    /**
     * Signals the receiver that the caller has completed one batch. The receiver must send its accumulated state to downstream consumers.
     */
    void flush();

    /**
     * Flushes any pending state computed so far and closes this processor. The instance will not be usable afterwards.
     */
    @Override
    void close();
}
