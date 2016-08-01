package org.fluidity.wages.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.fluidity.wages.ShiftDetails;

/**
 * Represents a work shift by a particular person on a particular local date during a certain time interval.
 */
final class WorkShift implements Comparable<WorkShift> {

    final String personId;
    final String personName;
    final LocalDate date;

    private final ZonedDateTimeInterval interval;

    /**
     * Creates a new instance.
     *
     * @param details  the shift details as specified via the API.
     * @param timeZone the time zone in which the shift has been performed.
     */
    WorkShift(final ShiftDetails details, final ZoneId timeZone) {
        this.personId = details.personId;
        this.personName = details.personName;
        this.date = details.date;
        this.interval = LocalTimeInterval.of(details.begin, details.end).locate(details.date, timeZone);
    }

    @Override
    public int compareTo(final WorkShift that) {
        int result;

        if (this == that) {
            result = 0;
        } else {
            result = this.personName.compareTo(that.personName);

            if (result == 0) {
                result = this.personId.compareTo(that.personId);

                if (result == 0) {
                    result = this.date.compareTo(that.date);

                    if (result == 0) {
                        result = this.start().compareTo(that.start());
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns a possibly empty duration that spans the overlapping period between the given interval and this one.
     *
     * @param that the other interval.
     *
     * @return a duration object; never <code>null</code>.
     */
    Duration overlap(final LocalTimeInterval that) {
        return this.interval.overlap(that.locate(date, start().getZone()));
    }

    /**
     * Returns the first day of the month in which this shift took place.
     *
     * @return a local date object; never <code>null</code>.
     */
    LocalDate month() {
        return LocalDate.of(date.getYear(), date.getMonth(), 1);
    }

    private ZonedDateTime start() {
        return this.interval.begin;
    }
}
