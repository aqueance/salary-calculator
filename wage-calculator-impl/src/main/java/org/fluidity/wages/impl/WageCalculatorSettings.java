package org.fluidity.wages.impl;

import java.time.ZoneId;

import org.fluidity.wages.WageCalculator;

/**
 * TODO: javadoc...
 */
public interface WageCalculatorSettings {

    /**
     * TODO
     * @return
     */
    ZoneId timeZone();

    /**
     * TODO
     * @return
     */
    RegularRatePeriod[] regularRates();

    /**
     * TODO
     * @return
     */
    WageCalculator.OvertimeRate[] overtimeRates();
}
