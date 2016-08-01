package org.fluidity.wages.cli;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.fluidity.composition.Component;
import org.fluidity.foundation.Configuration;
import org.fluidity.wages.ShiftDetails;

/**
 * Parses a CSV file line by line.
 * <p>
 * Use the {@link #create(Consumer)} method to create a new parser, implemented as a {@link Consumer} of Strings, and then feed each line of the CSV input
 * to the parser.
 * <p>
 * This parser assumes a certain set of fields in the CSV file, defined by {@link FieldName}. The first line of the CSV must be a list of field names,
 * configured with {@link Settings#fields()}.
 */
@Component
final class CsvParser {

    private static final DateTimeFormatter dates = DateTimeFormatter.ofPattern("d.M.yyyy");
    private static final DateTimeFormatter times = DateTimeFormatter.ofPattern("H:m");

    private enum FieldName {
        ID, NAME, DATE, START, STOP
    }

    private final Map<String, FieldName> fields = new HashMap<>();

    /**
     * Creates a new instance with some settings.
     *
     * @param configuration Encapsulates the settings.
     */
    CsvParser(final Configuration<Settings> configuration) {
        final Map<String, String> fields = configuration.settings().fields();

        // Reverse mapping
        for (final Map.Entry<String, String> entry : fields.entrySet()) {
            final String fieldName = entry.getValue().toUpperCase();
            this.fields.put(fieldName, FieldName.valueOf(entry.getKey().toUpperCase()));
        }
    }

    /**
     * Creates a new parser that sends parsed {@link ShiftDetails} objects to the given consumer.
     *
     * @param consumer The object to send parsed lines to.
     *
     * @return A consumer to feed CSV lines to.
     */
    Consumer<String> create(final Consumer<ShiftDetails> consumer) {
        return new Consumer<String>() {

            private int[] fieldMap;
            private boolean header = true;

            @Override
            public void accept(final String record) {
                final String[] fields = record.split(",");

                if (header) {
                    header = false;

                    assert fieldMap == null;
                    fieldMap = fieldMap(fields);
                } else {
                    assert fieldMap != null;

                    final FieldName[] names = FieldName.values();
                    final String[] values = new String[names.length];

                    for (int i = 0, ii = names.length; i < ii; ++i) {
                        values[i] = fields[fieldMap[names[i].ordinal()]].trim();
                    }

                    final ShiftDetails shift = new ShiftDetails(values[FieldName.ID.ordinal()],
                                                                values[FieldName.NAME.ordinal()],
                                                                LocalDate.parse(values[FieldName.DATE.ordinal()], dates),
                                                                LocalTime.parse(values[FieldName.START.ordinal()], times),
                                                                LocalTime.parse(values[FieldName.STOP.ordinal()], times));

                    consumer.accept(shift);
                }
            }

            /**
             * Takes a list of CSV field names (the CSV header) and maps each name to the constant defined in {@link FieldName}.
             *
             * @param names The list of names read from the CSV header.
             *
             * @return An integer array that maps {@link FieldName} objects by their {@link Enum#ordinal()} to the index in the value list in a CSV
             * line.
             */
            private int[] fieldMap(final String[] names) {
                final FieldName[] fieldNames = FieldName.values();
                final int[] map = new int[fieldNames.length];

                if (names.length != map.length) {
                    throw new IllegalArgumentException(String.format("unexpected CSV field count: %d (expecting %d: %s)",
                                                                     names.length,
                                                                     map.length,
                                                                     Arrays.toString(fieldNames)));
                }

                // Marks each slot to tell if a slot has been set or not.
                for (int i = 0, ii = names.length; i < ii; i++) {
                    map[i] = ~0;
                }

                // Maps the header field to a known field name.
                for (int i = 0, ii = names.length; i < ii; i++) {
                    final String fieldName = names[i];
                    final FieldName fieldConstant = CsvParser.this.fields.get(fieldName.toUpperCase());

                    if (fieldConstant == null) {
                        throw new IllegalArgumentException(String.format("CSV header '%s' not recognized", fieldName));
                    }

                    final int index = fieldConstant.ordinal();

                    if (map[index] != ~0) {
                        throw new IllegalArgumentException(String.format("CSV header '%s' encountered twice", fieldName));
                    }

                    map[index] = i;
                }

                return map;
            }
        };
    }

    /**
     * CSV parser settings.
     */
    interface Settings {

        /**
         * Lists the names of the known CSV fields, defined in {@link FieldName}.
         *
         * @return a map with {@link FieldName} names as keys and the CSV header names as values.
         */
        @Configuration.Property(key = "csv.fields", ids = "list")
        Map<String, String> fields();
    }
}
