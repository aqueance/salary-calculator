package org.fluidity.wages.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;

import org.fluidity.composition.Component;
import org.fluidity.wages.SalaryDetails;

/**
 * Encapsulates the logic of reading CSV from a {@link Reader} and calculating the wages therefrom.
 */
@Component
public final class SalaryCalculator {

    private final Parser parsers;
    private final org.fluidity.wages.SalaryCalculator.Factory calculators;

    public SalaryCalculator(final Parser parsers, final org.fluidity.wages.SalaryCalculator.Factory calculators) {
        this.parsers = parsers;
        this.calculators = calculators;
    }

    /**
     * Reads the shift details from the given reader and sends the computed salary details to the given consumer.
     *
     * @param reader   the reader to read CSV lines from.
     * @param consumer the consumer to send salary details to.
     *
     * @throws IOException when the reader throws the same.
     */
    public void process(final Reader reader, final Consumer<SalaryDetails> consumer) throws IOException {
        try (final BufferedReader content = new BufferedReader(reader); final org.fluidity.wages.SalaryCalculator calculator = calculators.create(consumer)) {
            content.lines().forEach(parsers.create(calculator));
        }
    }
}
