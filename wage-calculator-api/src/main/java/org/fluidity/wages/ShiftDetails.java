package org.fluidity.wages;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * TODO: javadoc...
 */
public final class ShiftDetails {

    public final String personId;
    public final LocalDate date;
    public final LocalTime begin;
    public final LocalTime end;

    public ShiftDetails(final String personId, final LocalDate date, final LocalTime begin, final LocalTime end) {
        this.personId = personId;
        this.date = date;
        this.begin = begin;
        this.end = end;
    }
}
