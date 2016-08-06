package org.fluidity.wages.impl;

/**
 * An duration after which some overtime rate applies.
 * <p>
 * This is an immutable value type.
 */
final class OvertimePercent {

    final int percent;
    final int thresholdMinutes;

    OvertimePercent(final int percent, final int thresholdHours, final int thresholdMinutes) {
        this.percent = percent;
        this.thresholdMinutes = thresholdHours * 60 + thresholdMinutes;
    }
}
