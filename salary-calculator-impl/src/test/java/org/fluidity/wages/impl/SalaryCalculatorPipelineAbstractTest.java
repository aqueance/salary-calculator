package org.fluidity.wages.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.fluidity.testing.Simulator;
import org.fluidity.wages.ShiftDetails;

@SuppressWarnings("WeakerAccess")
abstract class SalaryCalculatorPipelineAbstractTest extends Simulator {

    protected SalaryCalculatorPipeline.StageFactory createStageFactory(final SalaryCalculatorSettings settings) {
        return new SalaryCalculatorPipeline.StageFactory(settings);
    }

    protected SalaryCalculatorSettings settings(final String timeZone,
                                                final int baseRate,
                                                final List<RegularRatePeriod> regular,
                                                final List<OvertimePercent> overtime) {
        return new SalaryCalculatorSettings() {
            @Override
            public ZoneId timeZone() {
                return ZoneId.of(timeZone);
            }

            @Override
            public int baseRateBy100() {
                return baseRate;
            }

            @Override
            public List<RegularRatePeriod> regularRates() {
                return regular;
            }

            @Override
            public List<OvertimePercent> overtimeLevels() {
                return overtime;
            }
        };
    }

    /**
     * Creates a new regular rate period.
     *
     * @param rate  the hourly rate multiplied by 100 (precision).
     * @param begin the beginning of the period.
     * @param end   the end of the period.
     *
     * @return a new object; never <code>null</code>.
     */
    protected static RegularRatePeriod regularRate(final int rate, final LocalTime begin, final LocalTime end) {
        return new RegularRatePeriod(rate, begin, end);
    }

    /**
     * Creates a new overtime percent descriptor.
     *
     * @param percent     the percent amount of the base rate to apply.
     * @param fromHours   the number of hours after which the given percent applies.
     * @param fromMinutes the minutes of the number of hours after which the given percent applies.
     *
     * @return a new object; never <code>null</code>.
     */
    protected static OvertimePercent overtimeRate(final int percent, final int fromHours, final int fromMinutes) {
        return new OvertimePercent(percent, fromHours, fromMinutes);
    }

    /**
     * Creates a new work shift.
     *
     * @param timeZone   the time zone in which the work took place.
     * @param personId   the ID of the worker.
     * @param personName the name of the worker.
     * @param date       the date of the shift.
     * @param begin      the beginning of the shift.
     * @param end        the end of the shiftt
     *
     * @return a new object; never <code>null</code>.
     */
    protected WorkShift workShift(final ZoneId timeZone,
                                  final String personId,
                                  final String personName,
                                  final LocalDate date,
                                  final LocalTime begin,
                                  final LocalTime end) {
        return new WorkShift(new ShiftDetails(personId, personName, date, begin, end), timeZone);
    }
}
