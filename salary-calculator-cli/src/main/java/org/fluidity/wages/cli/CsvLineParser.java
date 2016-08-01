package org.fluidity.wages.cli;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.function.Function;

import org.fluidity.composition.Component;
import org.fluidity.wages.ShiftDetails;

/**
 * TODO
 */
@Component(api = CsvLineParser.class)
final class CsvLineParser implements Function<String, ShiftDetails> {

    private final DateTimeFormatter dates = DateTimeFormatter.ofPattern("d.M.yyyy");
    private final DateTimeFormatter times = DateTimeFormatter.ofPattern("H:m");

    @Override
    public ShiftDetails apply(final String record) {
        final String[] fields = record.split(",");

        return new ShiftDetails(fields[1].trim(),
                                fields[0].trim(),
                                dates.parse(fields[2].trim(), LocalDate::from),
                                times.parse(fields[3].trim(), LocalTime::from),
                                times.parse(fields[4].trim(), LocalTime::from));
    }
}
