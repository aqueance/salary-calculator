/*
 * Copyright (c) 2016 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
     * Specifies the base hourly rate multiplies by 100 (the dollar amount precision) to which other compensations are added.
     *
     * @return a number; greater than <code>0</code>.
     */
    int baseRateBy100();

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
    List<OvertimePercent> overtimeLevels();
}
