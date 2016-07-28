package org.fluidity.wages.impl;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * TODO: javadoc...
 */
final class ZonedDateTimeInterval {

    final ZonedDateTime begin;
    final ZonedDateTime end;

    ZonedDateTimeInterval(final ZonedDateTime begin, final ZonedDateTime end) {
        this.begin = begin;
        this.end = end;
    }

    /**
     * Computes the DST-aware length of the overlap between two time intervals.
     *
     * @param that the other time interval.
     *
     * @return the length of the overlap between the two time intervals; never <code>null</code>.
     */
    final Duration overlap(final ZonedDateTimeInterval that) {
        if (this.begin.isBefore(that.end) && that.begin.isBefore(this.end)) {
            final ZonedDateTime overlapBegin = (this.begin.isBefore(that.begin) ? that.begin : this.begin).withLaterOffsetAtOverlap();
            final ZonedDateTime overlapEnd = (this.end.isBefore(that.end) ? this.end : that.end).withLaterOffsetAtOverlap();

            return Duration.ofSeconds(overlapBegin.until(overlapEnd, ChronoUnit.SECONDS));
        } else {
            return Duration.ZERO;
        }
    }

    /**
     * TODO
     * @return
     */
    ZoneId timeZone() {
        return begin.getZone();
    }
}
