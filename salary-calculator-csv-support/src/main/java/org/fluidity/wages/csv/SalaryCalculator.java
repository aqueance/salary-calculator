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
