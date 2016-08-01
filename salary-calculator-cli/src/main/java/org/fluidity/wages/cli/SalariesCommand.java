package org.fluidity.wages.cli;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
import org.fluidity.wages.SalaryCalculator;
import org.fluidity.wages.SalaryDetails;

@Component
final class SalariesCommand implements Application {

    private static final String NAME = "Monthly salary calculator";

    private final CsvParser parsers;
    private final SalaryCalculator.Factory calculators;

    SalariesCommand(final CsvParser parsers, final SalaryCalculator.Factory calculators) {
        this.parsers = parsers;
        this.calculators = calculators;
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

    public void run(final String[] arguments) {
        if (arguments.length < 1) {
            usage("CSV file name missing");
            return;
        }

        if (arguments.length > 2) {
            usage("too many arguments");
        }

        final URL url;

        final String input = arguments[0];

        {   // determine if the input is a file or an URL
            final Path path = Paths.get(input);
            if (Files.exists(path)) {
                if (!Files.isReadable(path)) {
                    usage("file not readable: %s", path);
                    return;
                }

                try {
                    url = path.toUri().toURL();
                } catch (final MalformedURLException error) {
                    usage("conversion to URL failed: %s", path);
                    error.printStackTrace(System.err);
                    return;
                }
            } else {
                try {
                    url = new URL(input);
                } catch (final MalformedURLException error) {
                    usage("file not found: %s", path);
                    return;
                }
            }
        }

        final Charset encoding;

        {   // determine the character encoding to use
            if (arguments.length > 1) {
                        final String name = arguments[1];

                        try {
                            encoding = Charset.forName(name);
                        } catch (final UnsupportedCharsetException error) {
                            usage("unknown character encoding: %s", name);
                            return;
                        }
                    } else {
                        encoding = StandardCharsets.UTF_8;
                    }
        }

        // Prints the list of people with their salaries, with a header to identify the month
        final Consumer<SalaryDetails> printer = new Consumer<SalaryDetails>() {

            // The current month.
            private LocalDate month;

            @Override
            public void accept(final SalaryDetails details) {
                if (month == null || !details.month.equals(month)) {
                    month = details.month;

                    System.out.printf("Salaries for %d/%d:%n", month.getMonthValue(), month.getYear());
                }

                System.out.printf(" %s, %s, %s%n", details.personId, details.personName, details.amount());
            }
        };

        // This below is the actual logic; so far it was only preparation...

        try {
            final InputStream stream = url.openStream();
            final Reader reader = new InputStreamReader(stream, encoding);

            try (final BufferedReader content = new BufferedReader(reader); final SalaryCalculator calculator = calculators.create(printer)) {
                content.lines().forEach(parsers.create(calculator));
            }
        } catch (final Exception error) {
            usage("Error processing '%s': %s", input, error);
            error.printStackTrace(System.err);
        }
    }
}
