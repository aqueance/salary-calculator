package org.fluidity.wages.impl;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * A time interval from one date in some time zone to another date in some time zone. The time interval has a beginning and end.
 */
final class ZonedDateTimeInterval {

    final ZonedDateTime begin;
    final ZonedDateTime end;

    /**
     * Creates a new instance by specifying the beginning and the end of the time interval.
     *
     * @param begin the beginning of the time interval.
     * @param end   the end of the time interval.
     */
    ZonedDateTimeInterval(final ZonedDateTime begin, final ZonedDateTime end) {
        this.begin = begin;
        this.end = end;
    }

    /**
     * Computes the DST-aware length of the overlap between two intervals. On DST cutover when the time lines overlap, this method uses the later offset in all
     * cases.
     *
     * @param that the other interval.
     *
     * @return the length of the overlap between the two intervals; never <code>null</code>.
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
}
