package org.fluidity.wages.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import org.fluidity.wages.BatchProcessor;
import org.fluidity.wages.ShiftDetails;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RegularRatesStageTest extends SalaryCalculatorPipelineAbstractTest {

    @Test
    public void testSingleRateOneHourShiftInMiddle() throws Exception {
        final int whatever = 123;
        final int rate1By100 = 100;

        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           whatever,
                                                           Collections.singletonList(regularRate(rate1By100, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),
                                                           Collections.emptyList());

        final ShiftConsumer consumer = new ShiftConsumer();

        final BatchProcessor<WorkShift> subject = createStageFactory(settings).createRegularRatesStage(consumer);

        final ZoneId timeZone = settings.timeZone();
        final String personId = "1234";
        final LocalDate date = LocalDate.of(2000, Month.SEPTEMBER, 1);

        subject.accept(workShift(timeZone, personId, "John Doe", date, LocalTime.of(12, 0), LocalTime.of(13, 0)));

        subject.flush();

        Assert.assertEquals(consumer.rateBy6000, rate1By100 * 60);
    }

    @Test
    public void testSingleRateTwoOneHourShiftsAtEdges() throws Exception {
        final int whatever = 123;
        final int rate1By100 = 100;

        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           whatever,
                                                           Collections.singletonList(regularRate(rate1By100, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),
                                                           Collections.emptyList());

        final ShiftConsumer consumer = new ShiftConsumer();

        final BatchProcessor<WorkShift> subject = createStageFactory(settings).createRegularRatesStage(consumer);

        final ZoneId timeZone = settings.timeZone();
        final String personId = "1234";
        final LocalDate date = LocalDate.of(2000, Month.JANUARY, 1);

        subject.accept(workShift(timeZone, personId, "John Doe", date, LocalTime.of(0, 0), LocalTime.of(1, 0)));
        subject.accept(workShift(timeZone, personId, "John Doe", date, LocalTime.of(23, 0), LocalTime.of(0, 0)));

        subject.flush();

        Assert.assertEquals(consumer.rateBy6000, 2 * rate1By100 * 60);
    }

    @Test
    public void testSingleRateAllDayShift() throws Exception {
        final int whatever = 123;
        final int rate1By100 = 100;

        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           whatever,
                                                           Collections.singletonList(regularRate(rate1By100, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),
                                                           Collections.emptyList());

        final ShiftConsumer consumer = new ShiftConsumer();

        final BatchProcessor<WorkShift> subject = createStageFactory(settings).createRegularRatesStage(consumer);

        final ZoneId timeZone = settings.timeZone();
        final String personId = "1234";
        final LocalDate date = LocalDate.of(2000, Month.FEBRUARY, 1);

        subject.accept(workShift(timeZone, personId, "John Doe", date, LocalTime.of(0, 0), LocalTime.of(0, 0)));

        subject.flush();

        Assert.assertEquals(consumer.rateBy6000, 24 * rate1By100 * 60);
    }

    @Test
    public void testSingleRateAllDayShiftAtDSTGap() throws Exception {
        final int whatever = 123;
        final int rate1By100 = 100;

        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           whatever,
                                                           Collections.singletonList(regularRate(rate1By100, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),
                                                           Collections.emptyList());

        final ShiftConsumer consumer = new ShiftConsumer();

        final BatchProcessor<WorkShift> subject = createStageFactory(settings).createRegularRatesStage(consumer);

        final ZoneId timeZone = settings.timeZone();
        final String personId = "1234";
        final LocalDate date = LocalDate.of(2016, Month.MARCH, 27);

        subject.accept(workShift(timeZone, personId, "John Doe", date, LocalTime.of(0, 0), LocalTime.of(0, 0)));

        subject.flush();

        Assert.assertEquals(consumer.rateBy6000, 23 * rate1By100 * 60);
    }

    @Test
    public void testSingleRateAllDayShiftAtDSTOverlap() throws Exception {
        final int whatever = 123;
        final int rate1By100 = 100;

        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           whatever,
                                                           Collections.singletonList(regularRate(rate1By100, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),
                                                           Collections.emptyList());

        final ShiftConsumer consumer = new ShiftConsumer();

        final BatchProcessor<WorkShift> subject = createStageFactory(settings).createRegularRatesStage(consumer);

        final ZoneId timeZone = settings.timeZone();
        final String personId = "1234";
        final LocalDate date = LocalDate.of(2016, Month.OCTOBER, 30);

        subject.accept(workShift(timeZone, personId, "John Doe", date, LocalTime.of(0, 0), LocalTime.of(0, 0)));

        subject.flush();

        Assert.assertEquals(consumer.rateBy6000, 25 * rate1By100 * 60);
    }

    @Test
    public void testDualRateMultipleShiftsWithinPeriod() throws Exception {
        final int whatever = 123;
        final int rate1By100 = 37;
        final int rate2By100 = 73;

        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           whatever,
                                                           Arrays.asList(regularRate(rate2By100, LocalTime.MIDNIGHT, LocalTime.of(10, 0)),
                                                                         regularRate(rate1By100, LocalTime.of(10, 0), LocalTime.of(16, 0)),
                                                                         regularRate(rate2By100, LocalTime.of(16, 0), LocalTime.MIDNIGHT)),
                                                           Collections.emptyList());

        final ShiftConsumer consumer = new ShiftConsumer();

        final BatchProcessor<WorkShift> subject = createStageFactory(settings).createRegularRatesStage(consumer);

        final ZoneId timeZone = settings.timeZone();
        final String personId = "1234";
        final LocalDate date = LocalDate.of(2000, Month.JULY, 1);

        subject.accept(workShift(timeZone, personId, "John Doe", date, LocalTime.of(8, 0), LocalTime.of(9, 0)));      // 1 hour @ rate 2
        subject.accept(workShift(timeZone, personId, "John Doe", date, LocalTime.of(12, 0), LocalTime.of(13, 0)));    // 1 hour @ rate 1
        subject.accept(workShift(timeZone, personId, "John Doe", date, LocalTime.of(17, 0), LocalTime.of(18, 0)));    // 1 hour @ rate 2

        subject.flush();

        Assert.assertEquals(consumer.rateBy6000, (rate1By100 + 2 * rate2By100) * 60);
    }

    @Test
    public void testDualRateMultipleShiftsAtPeriodBoundary() throws Exception {
        final int whatever = 123;
        final int rate1By100 = 37;
        final int rate2By100 = 73;

        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           whatever,
                                                           Arrays.asList(regularRate(rate2By100, LocalTime.MIDNIGHT, LocalTime.of(10, 0)),
                                                                         regularRate(rate1By100, LocalTime.of(10, 0), LocalTime.of(16, 0)),
                                                                         regularRate(rate2By100, LocalTime.of(16, 0), LocalTime.MIDNIGHT)),
                                                           Collections.emptyList());

        final ShiftConsumer consumer = new ShiftConsumer();

        final BatchProcessor<WorkShift> subject = createStageFactory(settings).createRegularRatesStage(consumer);

        final ZoneId timeZone = settings.timeZone();
        final String personId = "1234";
        final LocalDate date = LocalDate.of(2000, Month.JULY, 1);

        subject.accept(workShift(timeZone, personId, "John Doe", date, LocalTime.of(9, 0), LocalTime.of(11, 0)));     // 1 + 1 hour @ each rate
        subject.accept(workShift(timeZone, personId, "John Doe", date, LocalTime.of(15, 0), LocalTime.of(17, 0)));    // 1 + 1 hour @ each rate

        subject.flush();

        Assert.assertEquals(consumer.rateBy6000, 2 * (rate1By100 + rate2By100) * 60);
    }

    private static class ShiftConsumer implements Consumer<SalaryCalculatorPipeline.ShiftSegment> {

        int rateBy6000;

        @Override
        public void accept(final SalaryCalculatorPipeline.ShiftSegment segment) {
            this.rateBy6000 += segment.minutes() * segment.rateBy100();
        }
    }
}
