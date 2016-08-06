package org.fluidity.wages.impl;

import org.fluidity.wages.SalaryCalculator;

/**
 * An duration after which some overtime rate applies.
 * <p>
 * This is an immutable value type.
 */
final class OvertimeRate {

    final int rateBy100;
    final int thresholdMinutes;

    OvertimeRate(final int rateBy100, final int thresholdHours, final int thresholdMinutes) {
        this.rateBy100 = rateBy100;
        this.thresholdMinutes = thresholdHours * 60 + thresholdMinutes;
    }
}
