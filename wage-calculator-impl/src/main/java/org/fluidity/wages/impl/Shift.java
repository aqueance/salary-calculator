package org.fluidity.wages.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.fluidity.wages.ShiftDetails;

/**
 * TODO
 */
final class Shift implements Comparable<Shift> {

    final String personId;
    final LocalDate date;

    private final ZonedDateTimeInterval interval;

    Shift(final ShiftDetails details, final ZoneId timeZone) {
        this.personId = details.personId;
        this.date = details.date;
        this.interval = new LocalTimeInterval(details.begin, details.end).locate(details.date, timeZone);
    }

    @Override
    public int compareTo(final Shift that) {
        int result = this.personId.compareTo(that.personId);

        if (result == 0) {
            result = this.date.compareTo(that.date);

            if (result == 0) {
                result = this.interval.begin.compareTo(that.interval.begin);
            }
        }

        return result;
    }

    Duration overlap(final LocalTimeInterval interval) {
        return this.interval.overlap(interval.locate(date, this.interval.timeZone()));
    }
}
