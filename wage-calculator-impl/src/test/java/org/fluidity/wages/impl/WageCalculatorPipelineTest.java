package org.fluidity.wages.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fluidity.testing.Simulator;
import org.fluidity.wages.ShiftDetails;
import org.fluidity.wages.WageCalculator;
import org.fluidity.wages.WageDetails;

import org.testng.Assert;
import org.testng.annotations.Test;

public final class WageCalculatorPipelineTest extends Simulator {

    private WageCalculatorSettings settings(final String timeZone, final List<RegularRatePeriod> regular, final List<WageCalculator.Settings.OvertimeRate> overtime) {
        return new WageCalculatorSettings() {
            @Override
            public ZoneId timeZone() {
                return ZoneId.of(timeZone);
            }

            @Override
            public RegularRatePeriod[] regularRates() {
                return regular.toArray(new RegularRatePeriod[regular.size()]);
            }

            @Override
            public WageCalculator.Settings.OvertimeRate[] overtimeRates() {
                return overtime.toArray(new WageCalculator.Settings.OvertimeRate[overtime.size()]);
            }
        };
    }

    @Test
    @SuppressWarnings({ "EmptyTryBlock", "unused" })
    public void acceptsEmptyList() throws Exception {
        final WageCalculatorSettings settings = settings("Europe/Helsinki",
                                                         Collections.singletonList(regularRate(100, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),
                                                         Collections.emptyList());

        final WageCalculatorFactory factory = new WageCalculatorFactory(settings);

        final List<WageDetails> wages = new ArrayList<>();

        verify(() -> {
            try (final WageCalculator subject = factory.create(wages::add)) {
                // empty
            }

            Assert.assertTrue(wages.isEmpty());
        });
    }

    @Test
    public void computesWagesForOneHourShift() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int regularRate = 100;
        final int eveningRate = 150;

        final int overtimeSchedule1Rate = 200;
        final int overtimeSchedule2Rate = 300;

        final WageCalculatorSettings settings = settings(zoneName,

                                                         // regular hours $1.00 from 10:00 to 15:00
                                                         // evening hours $1.50 from 15:00 to 10:00
                                                         Arrays.asList(regularRate(regularRate, LocalTime.of(10, 0), LocalTime.of(15, 0)),
                                                                       regularRate(eveningRate, LocalTime.of(15, 0), LocalTime.of(10, 0))),

                                                         // TODO: not used
                                                         // overtime compensation:
                                                         //  $2.00 from 4 hours
                                                         //  $3.00 from 6 hours
                                                         Arrays.asList(overtimeRate(overtimeSchedule1Rate, 4 * 60),
                                                                       overtimeRate(overtimeSchedule2Rate, 6 * 60))
        );

        final WageCalculatorFactory factory = new WageCalculatorFactory(settings);

        final List<WageDetails> wages = new ArrayList<>();

        final int year = 2000;
        final Month month = Month.JANUARY;
        final String personId = "1";

        final List<ShiftDetails> shifts = Collections.singletonList(
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month, 1),
                                 LocalTime.of(12, 0),
                                 LocalTime.of(13, 0))
        );

        verify(() -> {
            try (final WageCalculator subject = factory.create(wages::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(wages.size(), 1);

            final WageDetails details = wages.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100, regularRate);    // 1 hour on regular rate

            // verify the month
            Assert.assertEquals(details.date.getYear(), year);
            Assert.assertEquals(details.date.getMonth(), month);
            Assert.assertEquals(details.date.getDayOfMonth(), 1);
        });
    }

    private static RegularRatePeriod regularRate(final int rate, final LocalTime begin, final LocalTime end) {
        return new RegularRatePeriod(rate, begin, end);
    }

    private static WageCalculator.Settings.OvertimeRate overtimeRate(final int rate, final int fromMinutes) {
        return new WageCalculator.Settings.OvertimeRate() {
            @Override
            public int thresholdMinutes() {
                return fromMinutes;
            }

            @Override
            public int rateBy100() {
                return rate;
            }
        };
    }
}
