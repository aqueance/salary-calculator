package org.fluidity.wages.impl;

import java.time.ZoneId;
import java.util.List;

import org.fluidity.wages.SalaryCalculator;

/**
 * Runtime representation of a {@link SalaryCalculator.Settings} object.
 */
interface SalaryCalculatorSettings {

    /**
     * The time zone in which dates of the {@link org.fluidity.wages.ShiftDetails} objects are to be understood.
     *
     * @return a time zone object; never <code>null</code>.
     */
    ZoneId timeZone();

    /**
     * The list of regular hourly rates and the daily interval in which they apply.
     *
     * @return a list, never <code>null</code> or empty.
     */
    List<RegularRatePeriod> regularRates();

    /**
     * The list of overtime hourly rate levels and the number of minutes over which they apply.
     *
     * @return a list, never <code>null</code> but possibly empty.
     */
    List<OvertimeRate> overtimeRates();
}
