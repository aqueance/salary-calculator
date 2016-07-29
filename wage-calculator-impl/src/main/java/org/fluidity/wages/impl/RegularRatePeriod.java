package org.fluidity.wages.impl;

import java.time.LocalTime;

/**
 * An interval during which some regular hourly rate applies.
 * <p>
 * This is an immutable value type.
 */
final class RegularRatePeriod {

    final int rateBy100;
    final LocalTimeInterval interval;

    /**
     * Creates a new instance.
     *
     * @param rateBy100 the hourly rate multiplied by 100 (the precision for dollar amounts)
     * @param begin     the beginning of the interval.
     * @param end       the end of the interval.
     */
    RegularRatePeriod(final int rateBy100, final LocalTime begin, final LocalTime end) {
        this.rateBy100 = rateBy100;
        this.interval = LocalTimeInterval.of(begin, end);
    }
}
