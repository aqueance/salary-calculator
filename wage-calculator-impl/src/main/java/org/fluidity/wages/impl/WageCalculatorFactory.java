package org.fluidity.wages.impl;

import java.util.function.Consumer;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.wages.WageCalculator;
import org.fluidity.wages.WageDetails;

@Component
final class WageCalculatorFactory implements WageCalculator.Factory {

    private final ComponentContainer container;

    WageCalculatorFactory(final ComponentContainer container) {
        this.container = container;
    }

    @Override
    @SuppressWarnings("unchecked")
    public WageCalculator create(final Consumer<WageDetails> consumer) {
        return container.instantiate(WageCalculatorPipeline.class, registry -> registry.bindInstance(consumer, Consumer.class));
    }
}
