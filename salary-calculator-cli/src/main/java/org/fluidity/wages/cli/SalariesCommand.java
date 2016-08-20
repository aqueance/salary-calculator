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

package org.fluidity.wages.cli;

import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.function.Consumer;

import org.fluidity.composition.Component;
import org.fluidity.deployment.cli.Application;
import org.fluidity.foundation.Archives;
import org.fluidity.wages.SalaryDetails;
import org.fluidity.wages.csv.SalaryCalculator;

@Component
final class SalariesCommand implements Application {

    private static final String NAME = "Monthly salary calculator";

    private final SalaryCalculator calculator;

    SalariesCommand(final SalaryCalculator calculator) {
        this.calculator = calculator;
    }

    private void usage(final String error, final Object... arguments) {
        try {
            if (error != null) {
                System.err.printf("%s error: %s%n", SalariesCommand.NAME, String.format(error, arguments));
            } else {
                System.out.println(SalariesCommand.NAME);
            }

            System.out.println();
            System.out.printf("Usage: java -jar %s <CSV> [<encoding>]%n", Paths.get(Archives.root().toURI()).getFileName());
            System.out.println();
            System.out.println("  <CSV>:      The name of the input CSV to parse.");
            System.out.println("");
            System.out.println("              The first line of the CSV is its header, which is ignored.");
            System.out.println("              Subsequent lines have the following format:");
            System.out.println("");
            System.out.println("              name (text), ID (text), day (date), start (time), end (time)");
            System.out.println("");
            System.out.println("              Dates are formatted as day.month.year, each a number.");
            System.out.println("              Times are formatted as hour:minute, each a number.");
            System.out.println();
            System.out.println("  <encoding>: This is the character encoding of the CSV file.");
            System.out.println("              If not specified, UTF-8 will be used.");
            System.out.println();
        } catch (final URISyntaxException e) {
            assert false : e;
        }
    }

    public void run(final String... arguments) {
        if (arguments.length < 1) {
            usage("CSV file name missing");
            return;
        }

        if (arguments.length > 2) {
            usage("too many arguments");
        }

        final URL url;

        try {
            url = inputURL(arguments[0]);
        } catch (final IllegalStateException error) {
            error.printStackTrace(System.err);
            usage(null);
            return;
        } catch (final IllegalArgumentException error) {
            usage(error.getMessage());
            return;
        }

        final Charset encoding;

        try {
            encoding = arguments.length > 1 ? Charset.forName(arguments[1]) : StandardCharsets.UTF_8;
        } catch (final UnsupportedCharsetException error) {
            usage("unknown character encoding: %s", arguments[1]);
            return;
        }

        // Prints the list of people with their salaries, under a header that identifies the month
        final Consumer<SalaryDetails> printer = new Consumer<SalaryDetails>() {

            // The current month.
            private LocalDate month;

            @Override
            public void accept(final SalaryDetails details) {
                if (month == null || !details.month.equals(month)) {
                    month = details.month;

                    // The month header
                    System.out.printf("Salaries for %d/%d:%n", month.getMonthValue(), month.getYear());
                }

                // The person's salary details.
                System.out.printf(" %s, %s, %s%n", details.personId, details.personName, details.amount());
            }
        };

        // This below is the actual logic; up to here we were just preparing for this...

        try {
            calculator.process(new InputStreamReader(url.openStream(), encoding), printer);
        } catch (final Exception error) {
            usage("Error processing '%s': %s", url, error);
            error.printStackTrace(System.err);
        }
    }

    /**
     * Returns an URL that corresponds to the input, which is either a local file or a valid URL.
     *
     * @param input a file name or an URL.
     *
     * @return an URL; never <code>null</code>.
     *
     * @throws IllegalArgumentException when the given input is a file but does not exist or not readable, or an invalid URL.
     * @throws IllegalStateException    when the given input is a file but cannot be turned into an URL.
     */
    private URL inputURL(final String input) {
        final Path path = Paths.get(input);

        if (Files.exists(path)) {
            if (!Files.isReadable(path)) {
                throw new IllegalArgumentException(String.format("file not readable: %s", path));
            }

            try {
                return path.toUri().toURL();
            } catch (final MalformedURLException error) {
                throw new IllegalStateException(String.format("conversion to URL failed: %s", path));
            }
        } else {
            try {
                return new URL(input);
            } catch (final MalformedURLException error) {
                throw new IllegalArgumentException(String.format("file not found: %s", path));
            }
        }
    }
}
