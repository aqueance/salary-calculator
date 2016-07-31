package org.fluidity.wages.impl;

import java.time.ZoneId;
import java.util.List;

import org.fluidity.wages.WageCalculator;

/**
 * TODO: javadoc...
 */
interface WageCalculatorSettings {

    /**
     * TODO
     * @return
     */
    ZoneId timeZone();

    /**
     * TODO
     * @return
     */
    List<RegularRatePeriod> regularRates();

    /**
     * TODO
     * @return
     */
    List<WageCalculator.Settings.OvertimeRate> overtimeRates();
}
