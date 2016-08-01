package org.fluidity.wages.cli;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.fluidity.composition.Component;
import org.fluidity.deployment.cli.Application;
import org.fluidity.foundation.Archives;
import org.fluidity.wages.SalaryCalculator;

@Component
final class SalariesCommand implements Application {

    private static final String NAME = "Monthly salary calculator";

    private final CsvLineParser parser;
    private final SalaryCalculator.Factory calculators;

    SalariesCommand(final CsvLineParser parser, final SalaryCalculator.Factory calculators) {
        this.parser = parser;
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

        final String input = arguments[0];
        final Path path = Paths.get(input);

        if (!Files.exists(path)) {
            usage("file not found: %s", path);
            return;
        }

        if (!Files.isReadable(path)) {
            usage("file not readable: %s", path);
            return;
        }

        final Charset encoding;

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

        // TODO: support URLs for the CSV
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(input), encoding))) {
            try (final SalaryCalculator calculator = calculators.create(System.out::println)) {
                reader.lines()
                        .skip(1)        // skip the header
                        .map(parser)    // TODO: use the header
                        .forEach(calculator);
            }
        } catch (final Exception error) {
            usage("Error processing '%s': %s", path, error);
            error.printStackTrace(System.err);
        }
    }
}
