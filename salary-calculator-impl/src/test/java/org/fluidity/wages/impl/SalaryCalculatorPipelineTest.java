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
import org.fluidity.wages.Processor;
import org.fluidity.wages.SalaryCalculator;
import org.fluidity.wages.SalaryDetails;
import org.fluidity.wages.ShiftDetails;

import org.testng.Assert;
import org.testng.annotations.Test;

public final class SalaryCalculatorPipelineTest extends Simulator {

    private SalaryCalculatorSettings settings(final String timeZone, final List<RegularRatePeriod> regular, final List<SalaryCalculator.Settings.OvertimeRate> overtime) {
        return new SalaryCalculatorSettings() {
            @Override
            public ZoneId timeZone() {
                return ZoneId.of(timeZone);
            }

            @Override
            public List<RegularRatePeriod> regularRates() {
                return regular;
            }

            @Override
            public List<SalaryCalculator.Settings.OvertimeRate> overtimeRates() {
                return overtime;
            }
        };
    }

    @Test
    @SuppressWarnings({ "EmptyTryBlock", "unused" })
    public void acceptsEmptyList() throws Exception {
        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           Collections.singletonList(regularRate(100, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),
                                                           Collections.emptyList());

        final List<SalaryDetails> salary = new ArrayList<>();

        verify(() -> {
            try (final Processor<ShiftDetails> subject = new SalaryCalculatorPipeline(settings, salary::add)) {
                // empty
            }

            Assert.assertTrue(salary.isEmpty());
        });
    }

    @Test
    public void computesSalaryForOneHourShift() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int regularRate = 100;

        final SalaryCalculatorSettings settings = settings(zoneName,

                                                           // regular hours $1.00 all day
                                                           Collections.singletonList(regularRate(regularRate, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),

                                                           // no overtime
                                                           Collections.emptyList()
        );

        final List<SalaryDetails> salary = new ArrayList<>();

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
            try (final Processor<ShiftDetails> subject = new SalaryCalculatorPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100, regularRate);    // 1 hour on regular rate

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoOneHourShifts() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int regularRate = 100;

        final SalaryCalculatorSettings settings = settings(zoneName,

                                                           // regular hours $1.00 all day
                                                           Collections.singletonList(regularRate(regularRate, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),

                                                           // no overtime
                                                           Collections.emptyList()
        );

        final List<SalaryDetails> salary = new ArrayList<>();

        final int year = 2000;
        final Month month = Month.JANUARY;
        final String personId = "1";

        final List<ShiftDetails> shifts = Arrays.asList(
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month, 1),
                                 LocalTime.of(12, 0),
                                 LocalTime.of(13, 0)),
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month, 1),
                                 LocalTime.of(14, 0),
                                 LocalTime.of(15, 0))
        );

        verify(() -> {
            try (final Processor<ShiftDetails> subject = new SalaryCalculatorPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100, 2 * regularRate);    // 2 hours on regular rate

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForOneHourShiftsInTwoDays() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int regularRate = 100;

        final SalaryCalculatorSettings settings = settings(zoneName,

                                                           // regular hours $1.00 all day
                                                           Collections.singletonList(regularRate(regularRate, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),

                                                           // no overtime
                                                           Collections.emptyList()
        );

        final List<SalaryDetails> salary = new ArrayList<>();

        final int year = 2000;
        final Month month = Month.JANUARY;
        final String personId = "1";

        final List<ShiftDetails> shifts = Arrays.asList(
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month, 1),
                                 LocalTime.of(12, 0),
                                 LocalTime.of(13, 0)),
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month, 2),
                                 LocalTime.of(14, 0),
                                 LocalTime.of(15, 0))
        );

        verify(() -> {
            try (final Processor<ShiftDetails> subject = new SalaryCalculatorPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100, 2 * regularRate);    // 2 hours on regular rate

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoOvertimeLevelsWithinOneShift() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int regularRate = 100;
        final int eveningRate = 150;

        final int overtimeLevel1Rate = 200;
        final int overtimeLevel2Rate = 300;

        final SalaryCalculatorSettings settings = settings(zoneName,

                                                           // regular hours $1.00 from 10:00 to 15:00
                                                           // evening hours $1.50 from 15:00 to 10:00
                                                           Arrays.asList(regularRate(regularRate, LocalTime.of(10, 0), LocalTime.of(15, 0)),
                                                                       regularRate(eveningRate, LocalTime.of(15, 0), LocalTime.of(10, 0))),

                                                           // overtime compensation:
                                                           //  $2.00 from 4 hours
                                                           //  $3.00 from 6 hours
                                                           Arrays.asList(overtimeRate(overtimeLevel1Rate, 4 * 60),
                                                                       overtimeRate(overtimeLevel2Rate, 6 * 60))
        );

        final List<SalaryDetails> salary = new ArrayList<>();

        final int year = 2000;
        final Month month = Month.JANUARY;
        final String personId = "1";

        final List<ShiftDetails> shifts = Collections.singletonList(

                // 3 hours on regular shift = 3 hours regular rate
                // 4 hours on evening shift = 1 hour evening rate + 2 hours overtime level 1 + 1 hour overtime level 2
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month, 1),
                                 LocalTime.of(12, 0),
                                 LocalTime.of(19, 0))
        );

        verify(() -> {
            try (final Processor<ShiftDetails> subject = new SalaryCalculatorPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100, 3 * regularRate + eveningRate + 2 * overtimeLevel1Rate + overtimeLevel2Rate);

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoShiftsOnDifferentRegularRates() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int regularRate = 100;
        final int eveningRate = 150;

        final SalaryCalculatorSettings settings = settings(zoneName,

                                                           // regular hours $1.00 from 10:00 to 15:00
                                                           // evening hours $1.50 from 15:00 to 10:00
                                                           Arrays.asList(regularRate(regularRate, LocalTime.of(10, 0), LocalTime.of(15, 0)),
                                                                       regularRate(eveningRate, LocalTime.of(15, 0), LocalTime.of(10, 0))),

                                                           // no overtime
                                                           Collections.emptyList()
        );

        final List<SalaryDetails> salary = new ArrayList<>();

        final int year = 2000;
        final Month month = Month.JANUARY;
        final String personId = "1";

        final List<ShiftDetails> shifts = Arrays.asList(
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month, 1),
                                 LocalTime.of(10, 0),
                                 LocalTime.of(11, 0)),
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month, 2),
                                 LocalTime.of(15, 0),
                                 LocalTime.of(16, 0))
        );

        verify(() -> {
            try (final Processor<ShiftDetails> subject = new SalaryCalculatorPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100, regularRate + eveningRate);    // 1 hour on each rate

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoShiftsOnDifferentOvertimeRates() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int regularRate = 100;
        final int eveningRate = 150;

        final int overtimeLevel1Rate = 200;
        final int overtimeLevel2Rate = 300;

        final SalaryCalculatorSettings settings = settings(zoneName,

                                                           // regular hours $1.00 from 10:00 to 15:00
                                                           // evening hours $1.50 from 15:00 to 10:00
                                                           Arrays.asList(regularRate(regularRate, LocalTime.of(10, 0), LocalTime.of(15, 0)),
                                                                       regularRate(eveningRate, LocalTime.of(15, 0), LocalTime.of(10, 0))),

                                                           // overtime compensation:
                                                           //  $2.00 from 4 hours
                                                           //  $3.00 from 6 hours
                                                           Arrays.asList(overtimeRate(overtimeLevel1Rate, 4 * 60),
                                                                       overtimeRate(overtimeLevel2Rate, 6 * 60))
        );

        final List<SalaryDetails> salary = new ArrayList<>();

        final int year = 2000;
        final Month month = Month.JANUARY;
        final String personId = "1";

        final List<ShiftDetails> shifts = Arrays.asList(

                // 3 hours on regular rate + 1 hour evening rate + 2 hours overtime level 1
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month, 1),
                                 LocalTime.of(12, 0),
                                 LocalTime.of(18, 0)),

                // 1 hour overtime level 2
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month, 1),
                                 LocalTime.of(18, 0),
                                 LocalTime.of(19, 0))
        );

        verify(() -> {
            try (final Processor<ShiftDetails> subject = new SalaryCalculatorPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100, 3 * regularRate + eveningRate + 2 * overtimeLevel1Rate + overtimeLevel2Rate);

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoShiftsOnDifferentOvertimeRatesSkippingOneRegularLevel() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int regularRate = 100;
        final int eveningRate = 150;

        final int overtimeLevel1Rate = 200;
        final int overtimeLevel2Rate = 300;

        final SalaryCalculatorSettings settings = settings(zoneName,

                                                           // regular hours $1.00 from 10:00 to 15:00
                                                           // evening hours $1.50 from 15:00 to 10:00
                                                           Arrays.asList(regularRate(regularRate, LocalTime.of(10, 0), LocalTime.of(15, 0)),
                                                                       regularRate(eveningRate, LocalTime.of(15, 0), LocalTime.of(10, 0))),

                                                           // overtime compensation:
                                                           //  $2.00 from 4 hours
                                                           //  $3.00 from 6 hours
                                                           Arrays.asList(overtimeRate(overtimeLevel1Rate, 4 * 60),
                                                                       overtimeRate(overtimeLevel2Rate, 6 * 60))
        );

        final List<SalaryDetails> salary = new ArrayList<>();

        final int year = 2000;
        final Month month = Month.JANUARY;
        final String personId = "1";

        final List<ShiftDetails> shifts = Arrays.asList(

                // 4 hours on regular rate + 0 hour evening rate + 2 hours overtime level 1
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month, 1),
                                 LocalTime.of(11, 0),
                                 LocalTime.of(17, 0)),

                // 1 hour overtime level 2
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month, 1),
                                 LocalTime.of(18, 0),
                                 LocalTime.of(19, 0))
        );

        verify(() -> {
            try (final Processor<ShiftDetails> subject = new SalaryCalculatorPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100, 4 * regularRate + 2 * overtimeLevel1Rate + overtimeLevel2Rate);

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoPeople() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int regularRate = 100;
        final int eveningRate = 150;

        final SalaryCalculatorSettings settings = settings(zoneName,

                                                           // regular hours $1.00 from 10:00 to 15:00
                                                           // evening hours $1.50 from 15:00 to 10:00
                                                           Arrays.asList(regularRate(regularRate, LocalTime.of(10, 0), LocalTime.of(15, 0)),
                                                                       regularRate(eveningRate, LocalTime.of(15, 0), LocalTime.of(10, 0))),

                                                           // no overtime
                                                           Collections.emptyList()
        );

        final List<SalaryDetails> salary = new ArrayList<>();

        final int year = 2000;
        final Month month = Month.JANUARY;
        final String personId1 = "1";
        final String personId2 = "2";

        final List<ShiftDetails> shifts = Arrays.asList(
                new ShiftDetails(personId1,
                                 "John Doe",
                                 LocalDate.of(year, month, 1),
                                 LocalTime.of(12, 0),
                                 LocalTime.of(13, 0)),
                new ShiftDetails(personId2,
                                 "Jane Doe",
                                 LocalDate.of(year, month, 2),
                                 LocalTime.of(16, 0),
                                 LocalTime.of(17, 0))
        );

        verify(() -> {
            try (final Processor<ShiftDetails> subject = new SalaryCalculatorPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 2);

            final SalaryDetails details1 = salary.get(1);               // salary records are sorted by person name
            Assert.assertEquals(details1.personId, personId1);
            Assert.assertEquals(details1.amountBy100, regularRate);     // 1 hour on regular rate

            // verify the month
            Assert.assertEquals(details1.month.getYear(), year);
            Assert.assertEquals(details1.month.getMonth(), month);
            Assert.assertEquals(details1.month.getDayOfMonth(), 1);

            final SalaryDetails details2 = salary.get(0);               // salary records are sorted by person name
            Assert.assertEquals(details2.personId, personId2);
            Assert.assertEquals(details2.amountBy100, eveningRate);     // 1 hour on the evening rate

            // verify the month
            Assert.assertEquals(details2.month.getYear(), year);
            Assert.assertEquals(details2.month.getMonth(), month);
            Assert.assertEquals(details2.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoMonths() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int regularRate = 100;

        final SalaryCalculatorSettings settings = settings(zoneName,

                                                           // regular hours $1.00 all day
                                                           Collections.singletonList(regularRate(regularRate, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),

                                                           // no overtime
                                                           Collections.emptyList()
        );

        final List<SalaryDetails> salary = new ArrayList<>();

        final int year = 2000;
        final Month month1 = Month.JANUARY;
        final Month month2 = Month.FEBRUARY;
        final String personId = "1";

        final List<ShiftDetails> shifts = Arrays.asList(
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month1, 1),
                                 LocalTime.of(12, 0),
                                 LocalTime.of(13, 0)),
                new ShiftDetails(personId,
                                 "John Doe",
                                 LocalDate.of(year, month2, 1),
                                 LocalTime.of(14, 0),
                                 LocalTime.of(15, 0))
        );

        verify(() -> {
            try (final Processor<ShiftDetails> subject = new SalaryCalculatorPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 2);

            final SalaryDetails details1 = salary.get(0);
            Assert.assertEquals(details1.personId, personId);
            Assert.assertEquals(details1.amountBy100, regularRate);    // 1 hour on regular rate

            // verify the month
            Assert.assertEquals(details1.month.getYear(), year);
            Assert.assertEquals(details1.month.getMonth(), month1);
            Assert.assertEquals(details1.month.getDayOfMonth(), 1);

            final SalaryDetails details2 = salary.get(1);
            Assert.assertEquals(details2.personId, personId);
            Assert.assertEquals(details2.amountBy100, regularRate);    // 1 hour on regular rate

            // verify the month
            Assert.assertEquals(details2.month.getYear(), year);
            Assert.assertEquals(details2.month.getMonth(), month2);
            Assert.assertEquals(details2.month.getDayOfMonth(), 1);
        });
    }

    // TODO: test the DST cut-overs

    private static RegularRatePeriod regularRate(final int rate, final LocalTime begin, final LocalTime end) {
        return new RegularRatePeriod(rate, begin, end);
    }

    private static SalaryCalculator.Settings.OvertimeRate overtimeRate(final int rate, final int fromMinutes) {
        return new SalaryCalculator.Settings.OvertimeRate() {
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
