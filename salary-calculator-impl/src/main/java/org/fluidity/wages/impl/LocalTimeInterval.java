package org.fluidity.wages.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * A time interval without reference to a date or time zone. The time interval has a beginning and end. If the end is before the beginning, then it is
 * understood to be the following day.
 * <p>
 * This is an immutable value type.
 */
final class LocalTimeInterval {

    private final LocalTime begin;
    private final LocalTime end;

    /**
     * Creates a new instance by specifying the beginning and the end of the time interval.
     *
     * @param begin the beginning of the time interval.
     * @param end   the end of the time interval.
     */
    static LocalTimeInterval of(final LocalTime begin, final LocalTime end) {
        return new LocalTimeInterval(begin, end);
    }

    private LocalTimeInterval(final LocalTime begin, final LocalTime end) {
        this.begin = begin;
        this.end = end;
    }

    /**
     * Computes the DST-aware length of the overlap between two time intervals on the given date in the given time zone.
     *
     * @param that the other time interval.
     * @param date the date on which the time intervals are interpreted.
     *
     * @return the length of the overlap between the two time intervals; never <code>null</code>.
     *
     * @see ZonedDateTimeInterval#overlap(ZonedDateTimeInterval)
     */
    final Duration overlap(final LocalTimeInterval that, final LocalDate date, final ZoneId timeZone) {
        return this.locate(date, timeZone).overlap(that.locate(date, timeZone));
    }

    /**
     * Locates this time interval on the given date in the given time zone.
     *
     * @param date     the date in which to locate this interval.
     * @param timeZone the time zone in which to locate this interval.
     *
     * @return a new located interval; never <code>null</code>.
     */
    final ZonedDateTimeInterval locate(final LocalDate date, final ZoneId timeZone) {
        final ZonedDateTime intervalBegin = this.begin.atDate(date).atZone(timeZone);
        final ZonedDateTime intervalEnd = this.end.atDate(date).atZone(timeZone).plus(!this.end.isAfter(this.begin) ? 1 : 0, ChronoUnit.DAYS);

        return new ZonedDateTimeInterval(intervalBegin, intervalEnd);
    }
}
