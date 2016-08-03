package org.fluidity.wages;

/**
 * Wraps a {@link BatchProcessor} and ignores {@link BatchProcessor#flush()}. Use this to allow the {@link #flush()} method of the delegate to be invoked
 * independently of the previous stages in a {@link BatchProcessor} pipeline, which call the {@link #flush()} method of the wrapper from their corresponding
 * method.
 *
 * @param <T> the type of the input to the processor.
 */
public final class IndependentBatchControl<T> implements BatchProcessor<T> {

    private final BatchProcessor<T> delegate;

    public IndependentBatchControl(final BatchProcessor<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void flush() {
        // ignored, which is the purpose of this proxy
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void accept(final T input) {
        delegate.accept(input);
    }
}
