package org.fluidity.wages.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.IntConsumer;

import org.fluidity.wages.BatchProcessor;

import org.testng.Assert;
import org.testng.annotations.Test;

public class OvertimeRatesStageTest extends SalaryCalculatorPipelineAbstractTest {

    @Test
    public void testNoOvertimeRate() throws Exception {
        final LocalDate date = LocalDate.of(2000, Month.JANUARY, 1);

        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           0,
                                                           Collections.singletonList(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),
                                                           Collections.emptyList());

        final SalaryConsumer consumer = new SalaryConsumer();

        final BatchProcessor<SalaryCalculatorPipeline.ShiftSegment> subject = createStageFactory(settings).createOvertimeRatesStage(consumer);

        subject.accept(shiftSegment(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT), date, LocalTime.of(10, 0), LocalTime.of(16, 0), settings.timeZone()));

        subject.flush();

        Assert.assertEquals(consumer.amountBy100, 0);
    }

    @Test
    public void testSingleOvertimeRateWithHoursUnder() throws Exception {
        final int rate1By100 = 100;
        final LocalDate date = LocalDate.of(2000, Month.JANUARY, 1);

        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           0,
                                                           Collections.singletonList(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),
                                                           Collections.singletonList(overtimeRate(rate1By100, 4, 0)));

        final SalaryConsumer consumer = new SalaryConsumer();

        final BatchProcessor<SalaryCalculatorPipeline.ShiftSegment> subject = createStageFactory(settings).createOvertimeRatesStage(consumer);

        subject.accept(shiftSegment(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT), date, LocalTime.of(10, 0), LocalTime.of(12, 0), settings.timeZone()));

        subject.flush();

        Assert.assertEquals(consumer.amountBy100, 0);
    }

    @Test
    public void testSingleOvertimeRateWithHoursAt() throws Exception {
        final int rate1By100 = 100;
        final LocalDate date = LocalDate.of(2000, Month.JANUARY, 1);

        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           0,
                                                           Collections.singletonList(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),
                                                           Collections.singletonList(overtimeRate(rate1By100, 4, 0)));

        final SalaryConsumer consumer = new SalaryConsumer();

        final BatchProcessor<SalaryCalculatorPipeline.ShiftSegment> subject = createStageFactory(settings).createOvertimeRatesStage(consumer);

        subject.accept(shiftSegment(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT), date, LocalTime.of(10, 0), LocalTime.of(14, 0), settings.timeZone()));

        subject.flush();

        Assert.assertEquals(consumer.amountBy100, 0);
    }

    @Test
    public void testSingleOvertimeRateWithHoursOver() throws Exception {
        final int percent1 = 100;
        final LocalDate date = LocalDate.of(2000, Month.JANUARY, 1);

        final int threshold1 = 4;
        final int baseRate = 100;
        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           baseRate,
                                                           Collections.singletonList(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),
                                                           Collections.singletonList(overtimeRate(percent1, threshold1, 0)));

        final SalaryConsumer consumer = new SalaryConsumer();

        final BatchProcessor<SalaryCalculatorPipeline.ShiftSegment> subject = createStageFactory(settings).createOvertimeRatesStage(consumer);

        final int hours = 8;

        subject.accept(shiftSegment(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT),
                                    date,
                                    LocalTime.of(10, 0),
                                    LocalTime.of(10 + hours, 0),
                                    settings.timeZone()));

        subject.flush();

        Assert.assertEquals(consumer.amountBy100,
                            (baseRate * hours +
                             Math.round((float) baseRate * (float) (hours - threshold1) * (float) percent1 / (float) 100)));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMultipleOvertimeRates() throws Exception {
        final int percent1 = 12;
        final int percent2 = 34;
        final LocalDate date = LocalDate.of(2000, Month.JANUARY, 1);

        final int threshold1 = 4;
        final int threshold2 = 6;
        final int baseRate = 100;
        final SalaryCalculatorSettings settings = settings("Europe/Helsinki",
                                                           baseRate,
                                                           Collections.singletonList(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)),
                                                           Arrays.asList(overtimeRate(percent1, threshold1, 0),
                                                                         overtimeRate(percent2, threshold2, 0)));

        final SalaryConsumer consumer = new SalaryConsumer();

        final BatchProcessor<SalaryCalculatorPipeline.ShiftSegment> subject = createStageFactory(settings).createOvertimeRatesStage(consumer);

        final int hours = 8;

        subject.accept(shiftSegment(regularRate(0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT),
                                    date,
                                    LocalTime.of(10, 0),
                                    LocalTime.of(10 + hours, 0),
                                    settings.timeZone()));

        subject.flush();

        assert hours > threshold2;
        assert threshold2 > threshold1;

        Assert.assertEquals(consumer.amountBy100,
                            (baseRate * hours +
                             Math.round((float) baseRate * (float) (hours - threshold2) * (float) percent2 / (float) 100) +
                             Math.round((float) baseRate * (float) (threshold2 - threshold1) * (float) percent1 / (float) 100)));
    }

    private SalaryCalculatorPipeline.ShiftSegment shiftSegment(final RegularRatePeriod period, final LocalDate date,
                                                               final LocalTime begin,
                                                               final LocalTime end,
                                                               final ZoneId timeZone) {
        final SalaryCalculatorPipeline.ShiftSegment segment = new SalaryCalculatorPipeline.ShiftSegment(period);
        segment.accept(workShift(timeZone, "any1", "any 2", date, begin, end));
        return segment;
    }

    private static class SalaryConsumer implements IntConsumer {

        int amountBy100;

        @Override
        public void accept(final int amountBy100) {
            this.amountBy100 += amountBy100;
        }
    }
}
