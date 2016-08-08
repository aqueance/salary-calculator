package org.fluidity.wages.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.fluidity.wages.BatchProcessor;
import org.fluidity.wages.SalaryDetails;
import org.fluidity.wages.ShiftDetails;

import org.testng.Assert;
import org.testng.annotations.Test;

public final class SalaryCalculatorPipelineTest extends SalaryCalculatorPipelineAbstractTest {

    /**
     * Creates a new subject to test.
     *
     * @param settings the settings to use.
     * @param consumer the consumer to use.
     *
     * @return a new subject; never <code>null</code>.
     */
    private SalaryCalculatorPipeline createPipeline(final SalaryCalculatorSettings settings, final Consumer<SalaryDetails> consumer) {
        return new SalaryCalculatorPipeline(createStageFactory(settings), settings, consumer);
    }

    @Test
    @SuppressWarnings({ "EmptyTryBlock", "unused" })
    public void acceptsEmptyList() throws Exception {
        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           100,
                                                           Collections.singletonList(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),
                                                           Collections.emptyList());

        final List<SalaryDetails> salary = new ArrayList<>();

        verify(() -> {
            try (final BatchProcessor<ShiftDetails> subject = createPipeline(settings, salary::add)) {
                // empty
            }

            Assert.assertTrue(salary.isEmpty());
        });
    }

    @Test
    public void computesSalaryForOneHourShift() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int baseRate = 100;

        final SalaryCalculatorSettings settings = settings(zoneName,
                                                           baseRate,

                                                           // regular hours $1.00 all day
                                                           Collections.singletonList(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),

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
            try (final BatchProcessor<ShiftDetails> subject = createPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100, baseRate);    // 1 hour on regular rate

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoOneHourShifts() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int baseRate = 100;

        final SalaryCalculatorSettings settings = settings(zoneName,
                                                           baseRate,

                                                           // regular hours $1.00 all day
                                                           Collections.singletonList(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),

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
            try (final BatchProcessor<ShiftDetails> subject = createPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100, 2 * baseRate);    // 2 hours on regular rate

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForOneHourShiftsInTwoDays() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int baseRate = 100;

        final SalaryCalculatorSettings settings = settings(zoneName,
                                                           baseRate,

                                                           // regular hours $1.00 all day
                                                           Collections.singletonList(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),

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
            try (final BatchProcessor<ShiftDetails> subject = createPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100, 2 * baseRate);    // 2 hours on regular rate

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoOvertimeLevelsWithinOneShift() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int baseRate = 100;
        final int eveningRate = 50;

        final int overtimeLevel1Percent = 20;
        final int overtimeLevel2Percent = 30;

        final SalaryCalculatorSettings settings = settings(zoneName,
                                                           baseRate,

                                                           // regular hours $1.00 from 10:00 to 15:00
                                                           // evening hours $1.50 from 15:00 to 10:00
                                                           Arrays.asList(regularRate(eveningRate, LocalTime.MIDNIGHT, LocalTime.of(10, 0)),
                                                                         regularRate(0, LocalTime.of(10, 0), LocalTime.of(15, 0)),
                                                                         regularRate(eveningRate, LocalTime.of(15, 0), LocalTime.MIDNIGHT)),

                                                           // overtime compensation:
                                                           //  +20% from 4 hours
                                                           //  +30% from 6 hours
                                                           Arrays.asList(overtimeRate(overtimeLevel1Percent, 4, 0),
                                                                         overtimeRate(overtimeLevel2Percent, 6, 0))
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
            try (final BatchProcessor<ShiftDetails> subject = createPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100,
                                Math.round(3 * baseRate +
                                           (baseRate + eveningRate) +
                                           2 * ((float) (baseRate + eveningRate) + (float) baseRate * (float) overtimeLevel1Percent / (float) 100) +
                                                (float) (baseRate + eveningRate) + (float) baseRate * (float) overtimeLevel2Percent / (float) 100));

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoShiftsOnDifferentRegularRates() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int baseRate = 100;
        final int eveningRate = 50;

        final SalaryCalculatorSettings settings = settings(zoneName,
                                                           baseRate,

                                                           // regular hours $1.00 from 10:00 to 15:00
                                                           // evening hours $1.50 from 15:00 to 10:00
                                                           Arrays.asList(regularRate(eveningRate, LocalTime.MIDNIGHT, LocalTime.of(10, 0)),
                                                                         regularRate(0, LocalTime.of(10, 0), LocalTime.of(15, 0)),
                                                                         regularRate(eveningRate, LocalTime.of(15, 0), LocalTime.MIDNIGHT)),

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
            try (final BatchProcessor<ShiftDetails> subject = createPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100, baseRate +
                                                     baseRate + eveningRate);    // 1 hour on each rate

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoShiftsOnDifferentOvertimeRates() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int baseRate = 100;
        final int eveningRate = 50;

        final int overtimeLevel1Percent = 20;
        final int overtimeLevel2Percent = 30;

        final SalaryCalculatorSettings settings = settings(zoneName,
                                                           baseRate,

                                                           // regular hours $1.00 from 10:00 to 15:00
                                                           // evening hours $1.50 from 15:00 to 10:00
                                                           Arrays.asList(regularRate(eveningRate, LocalTime.MIDNIGHT, LocalTime.of(10, 0)),
                                                                         regularRate(0, LocalTime.of(10, 0), LocalTime.of(15, 0)),
                                                                         regularRate(eveningRate, LocalTime.of(15, 0), LocalTime.MIDNIGHT)),

                                                           // overtime compensation:
                                                           //  +20% from 4 hours
                                                           //  +30% from 6 hours
                                                           Arrays.asList(overtimeRate(overtimeLevel1Percent, 4, 0),
                                                                         overtimeRate(overtimeLevel2Percent, 6, 0))
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
            try (final BatchProcessor<ShiftDetails> subject = createPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100,
                                Math.round(3 * baseRate +
                                           (baseRate + eveningRate) +
                                           2 * ((float) (baseRate + eveningRate) + (float) baseRate * (float) overtimeLevel1Percent / (float) 100) +
                                           (float) (baseRate + eveningRate) + (float) baseRate * (float) overtimeLevel2Percent / (float) 100));

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoShiftsOnDifferentOvertimeRatesSkippingOneRegularLevel() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int baseRate = 100;
        final int eveningRate = 50;

        final int overtimeLevel1Percent = 20;
        final int overtimeLevel2Percent = 30;

        final SalaryCalculatorSettings settings = settings(zoneName,
                                                           baseRate,

                                                           // regular hours $1.00 from 10:00 to 15:00
                                                           // evening hours $1.50 from 15:00 to 10:00
                                                           Arrays.asList(regularRate(eveningRate, LocalTime.MIDNIGHT, LocalTime.of(10, 0)),
                                                                         regularRate(0, LocalTime.of(10, 0), LocalTime.of(15, 0)),
                                                                         regularRate(eveningRate, LocalTime.of(15, 0), LocalTime.MIDNIGHT)),

                                                           // overtime compensation:
                                                           //  +20% from 4 hours
                                                           //  +30% from 6 hours
                                                           Arrays.asList(overtimeRate(overtimeLevel1Percent, 4, 0),
                                                                         overtimeRate(overtimeLevel2Percent, 6, 0))
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
            try (final BatchProcessor<ShiftDetails> subject = createPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 1);

            final SalaryDetails details = salary.get(0);
            Assert.assertEquals(details.personId, personId);
            Assert.assertEquals(details.amountBy100,
                                Math.round(4 * baseRate +
                                           2 * ((float) (baseRate + eveningRate) + (float) baseRate * (float) overtimeLevel1Percent / (float) 100) +
                                           (float) (baseRate + eveningRate) + (float) baseRate * (float) overtimeLevel2Percent / (float) 100));

            // verify the month
            Assert.assertEquals(details.month.getYear(), year);
            Assert.assertEquals(details.month.getMonth(), month);
            Assert.assertEquals(details.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoPeople() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int baseRate = 100;
        final int eveningRate = 50;

        final SalaryCalculatorSettings settings = settings(zoneName,
                                                           baseRate,

                                                           // regular hours $1.00 from 10:00 to 15:00
                                                           // evening hours $1.50 from 15:00 to 10:00
                                                           Arrays.asList(regularRate(eveningRate, LocalTime.MIDNIGHT, LocalTime.of(10, 0)),
                                                                         regularRate(0, LocalTime.of(10, 0), LocalTime.of(15, 0)),
                                                                         regularRate(eveningRate, LocalTime.of(15, 0), LocalTime.MIDNIGHT)),

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
            try (final BatchProcessor<ShiftDetails> subject = createPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 2);

            final SalaryDetails details1 = salary.get(1);               // salary records are sorted by person name
            Assert.assertEquals(details1.personId, personId1);
            Assert.assertEquals(details1.amountBy100, baseRate);        // 1 hour on regular rate

            // verify the month
            Assert.assertEquals(details1.month.getYear(), year);
            Assert.assertEquals(details1.month.getMonth(), month);
            Assert.assertEquals(details1.month.getDayOfMonth(), 1);

            final SalaryDetails details2 = salary.get(0);               // salary records are sorted by person name
            Assert.assertEquals(details2.personId, personId2);
            Assert.assertEquals(details2.amountBy100, baseRate + eveningRate);     // 1 hour on the evening rate

            // verify the month
            Assert.assertEquals(details2.month.getYear(), year);
            Assert.assertEquals(details2.month.getMonth(), month);
            Assert.assertEquals(details2.month.getDayOfMonth(), 1);
        });
    }

    @Test
    public void computesSalaryForTwoMonths() throws Exception {
        final String zoneName = "Europe/Helsinki";

        final int baseRate = 100;

        final SalaryCalculatorSettings settings = settings(zoneName,
                                                           baseRate,

                                                           // regular hours $1.00 all day
                                                           Collections.singletonList(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),

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
            try (final BatchProcessor<ShiftDetails> subject = createPipeline(settings, salary::add)) {
                shifts.forEach(subject);
            }

            Assert.assertEquals(salary.size(), 2);

            final SalaryDetails details1 = salary.get(0);
            Assert.assertEquals(details1.personId, personId);
            Assert.assertEquals(details1.amountBy100, baseRate);    // 1 hour on base rate

            // verify the month
            Assert.assertEquals(details1.month.getYear(), year);
            Assert.assertEquals(details1.month.getMonth(), month1);
            Assert.assertEquals(details1.month.getDayOfMonth(), 1);

            final SalaryDetails details2 = salary.get(1);
            Assert.assertEquals(details2.personId, personId);
            Assert.assertEquals(details2.amountBy100, baseRate);    // 1 hour on base rate

            // verify the month
            Assert.assertEquals(details2.month.getYear(), year);
            Assert.assertEquals(details2.month.getMonth(), month2);
            Assert.assertEquals(details2.month.getDayOfMonth(), 1);
        });
    }

    // TODO: test the DST cut-overs
    // TODO: test flushing the pipeline
}
