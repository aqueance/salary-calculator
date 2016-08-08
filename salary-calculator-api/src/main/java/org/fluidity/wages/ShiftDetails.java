package org.fluidity.wages;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a single work shift.
 * <p>
 * This is an immutable value type.
 */
public final class ShiftDetails {

    public final String personId;
    public final String personName;
    public final LocalDate date;
    public final LocalTime begin;
    public final LocalTime end;

    public ShiftDetails(final String personId, final String personName, final LocalDate date, final LocalTime begin, final LocalTime end) {
        this.personId = personId;
        this.personName = personName;
        this.date = date;
        this.begin = begin;
        this.end = end;
    }
}
