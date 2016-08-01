package org.fluidity.wages.impl;

import java.util.function.Consumer;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.wages.SalaryCalculator;
import org.fluidity.wages.SalaryDetails;

@Component
final class SalaryCalculatorFactory implements SalaryCalculator.Factory {

    private final ComponentContainer container;

    SalaryCalculatorFactory(final ComponentContainer container) {
        this.container = container;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SalaryCalculator create(final Consumer<SalaryDetails> consumer) {
        return container.instantiate(SalaryCalculatorPipeline.class, registry -> registry.bindInstance(consumer, Consumer.class));
    }
}
