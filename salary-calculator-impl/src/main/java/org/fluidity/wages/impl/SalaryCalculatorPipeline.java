package org.fluidity.wages.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import org.fluidity.composition.Component;
import org.fluidity.wages.BatchProcessor;
import org.fluidity.wages.SalaryCalculator;
import org.fluidity.wages.SalaryDetails;
import org.fluidity.wages.ShiftDetails;

/**
 * Implements the salary calculator as a pipeline of auto-closeable consumers. The {@link AutoCloseable#close()} method flushes the pipeline.
 * <p>
 * Instances are created by {@link SalaryCalculator.Factory#create(Consumer)}.
 */
@Component(automatic = false)
final class SalaryCalculatorPipeline implements SalaryCalculator {

    private final ZoneId timeZone;

    private final StageFactory stages;
    private final Consumer<SalaryDetails> consumer;

    // Sorts work shift objects to form the input of the pipeline.
    private final Collection<WorkShift> shifts = new TreeSet<>();

    private PersonDetails person;
    private int day;

    SalaryCalculatorPipeline(final StageFactory stages, final SalaryCalculatorSettings settings, final Consumer<SalaryDetails> consumer) {
        this.timeZone = settings.timeZone();
        this.stages = stages;
        this.consumer = consumer;
    }

    @Override
    public void accept(final ShiftDetails details) {
        shifts.add(new WorkShift(details, timeZone));
    }

    @Override
    public void flush() {
        final IntConsumer stage3 = amountBy100 -> person.addSalary(amountBy100);
        final BatchProcessor<ShiftSegment> stage2 = stages.createOvertimeRatesStage(stage3);
        final BatchProcessor<WorkShift> stage1 = stages.createRegularRatesStage(stage2);

        try (final BatchProcessor<WorkShift> pipeline = new MultiStagePipeline<>(stage1, stage2)) {
            shifts.forEach(shift -> {
                final int day = shift.date.getDayOfMonth();

                final boolean atMonthOrPersonBoundary = this.person == null || !this.person.matches(shift);
                final boolean atDayBoundary = atMonthOrPersonBoundary || day != this.day;

                // this must be handled first as the smaller granularity
                if (atDayBoundary) {
                    if (this.day > 0) {
                        pipeline.flush();
                    }

                    this.day = day;
                }

                if (atMonthOrPersonBoundary) {
                    if (this.person != null) {
                        this.consumer.accept(this.person.salary());
                    }

                    this.person = new PersonDetails(shift);
                }

                pipeline.accept(shift);
            });

            if (this.person != null) {
                assert this.day > 0;
                pipeline.flush();
                this.consumer.accept(this.person.salary());
            }
        }

        reset();
    }

    @Override
    public void close() {
        flush();
    }

    private void reset() {
        this.shifts.clear();
        this.day = 0;
        this.person = null;
    }

    /**
     * Creates the various stages of the pipeline.
     */
    @Component
    final static class StageFactory {

        private final SalaryCalculatorSettings settings;

        StageFactory(final SalaryCalculatorSettings settings) {
            this.settings = settings;
        }

        /**
         * Creates the stage that assigns regular rates to periods of the shifts.
         *
         * @param consumer the next stage in the pipeline.
         *
         * @return a new processor; never <code>null</code>.
         */
        BatchProcessor<WorkShift> createRegularRatesStage(final Consumer<ShiftSegment> consumer) {
            return new RegularRatesStage(settings, consumer);
        }

        /**
         * Creates the stage that tracks overtime.
         *
         * @param consumer the next stage in the pipeline.
         *
         * @return a new processor; never <code>null</code>.
         */
        BatchProcessor<ShiftSegment> createOvertimeRatesStage(final IntConsumer consumer) {
            return new OvertimeRatesStage(settings, consumer);
        }
    }

    /**
     * Broadcasts the {@link BatchProcessor#flush()} and {@link BatchProcessor#close()} methods to a list of {@link BatchProcessor} instances in sequence.
     *
     * @param <T> the consumer type of the first instance.
     */
    private static class MultiStagePipeline<T> implements BatchProcessor<T> {

        private final BatchProcessor<T> stage1;
        private final BatchProcessor[] rest;

        MultiStagePipeline(final BatchProcessor<T> stage1, final BatchProcessor ...rest) {
            this.stage1 = stage1;
            this.rest = rest;
        }

        @Override
        public void flush() {
            stage1.flush();

            for (final BatchProcessor stage : rest) {
                stage.flush();
            }
        }

        @Override
        public void close() {
            stage1.close();

            for (final BatchProcessor stage : rest) {
                stage.close();
            }
        }

        @Override
        public void accept(final T shift) {
            stage1.accept(shift);
        }
    }

    /**
     * Keeps track of the person whose work shifts are currently being processed.
     */
    private static final class PersonDetails {

        private final String personId;
        private final String personName;
        private final LocalDate month;

        private int salaryBy100;

        /**
         * Creates a new instance with the details of the person the given work shift belongs to.
         *
         * @param shift the work shift.
         */
        PersonDetails(final WorkShift shift) {
            this.personId = shift.personId;
            this.personName = shift.personName;
            this.month = shift.month();
        }

        /**
         * Checks if this instance maintains details about the person that the given shift applies to.
         *
         * @param shift the work shift to check.
         *
         * @return <code>true</code> if the given shift applies to the person being maintained by the receiver; <code>false</code> otherwise
         */
        boolean matches(final WorkShift shift) {
            return this.personId.equals(shift.personId) && this.month.equals(shift.month());
        }

        /**
         * Adds the given amount to this person's salary.
         *
         * @param amountBy100 the salary amount to return, multiplied by 100.
         */
        void addSalary(final int amountBy100) {
            salaryBy100 += amountBy100;
        }

        /**
         * Returns the {@link SalaryDetails} object for this person with the accumulated monthly salary.
         *
         * @return a new {@link SalaryDetails} object; never <code>null</code>.
         */
        SalaryDetails salary() {
            return new SalaryDetails(this.personId, this.personName, this.month, salaryBy100);
        }
    }

    /**
     * Computes the overlap between the daily shifts of a person and the regular hourly rate periods. For each regular rate period, there is a shift segment
     * that maintains the number of minutes it overlaps with the daily shifts, and the hourly rate for those minutes.
     */
    private static final class RegularRatesStage implements BatchProcessor<WorkShift> {

        private final Consumer<ShiftSegment> next;
        private final List<ShiftSegment> segments;

        private boolean dirty;

        RegularRatesStage(final SalaryCalculatorSettings settings, final Consumer<ShiftSegment> next) {
            this.next = next;

            final List<RegularRatePeriod> periods = settings.regularRates();
            assert !periods.isEmpty();
            assert periods.get(0).interval.begin.equals(LocalTime.MIDNIGHT) : periods.get(0).interval.begin;
            assert periods.get(periods.size() - 1).interval.end.equals(LocalTime.MIDNIGHT) : periods.get(periods.size() - 1).interval.end;

            this.segments = Collections.unmodifiableList(periods.stream()
                                                                 .map(ShiftSegment::new)
                                                                 .collect(Collectors.toList()));
        }

        @Override
        public void accept(final WorkShift shift) {
            segments.forEach(segment -> segment.accept(shift));
            dirty = true;
        }

        @Override
        public void flush() {
            if (dirty) {
                segments.forEach(next);
                segments.forEach(ShiftSegment::reset);

                dirty = false;
            }
        }

        @Override
        public void close() {
            flush();
        }
    }

    /**
     * Calculates the monthly salary from the consumed daily shifts.
     */
    private static final class OvertimeRatesStage implements BatchProcessor<ShiftSegment> {

        private final IntConsumer next;
        private final int baseRateBy100;
        private final List<OvertimePercent> overtimePercents;

        private BatchState state;

        /**
         * Creates a new instance.
         *
         * @param next the consumer for the computed salary.
         */
        OvertimeRatesStage(final SalaryCalculatorSettings settings, final IntConsumer next) {
            this.next = next;
            this.overtimePercents = settings.overtimeLevels();
            this.baseRateBy100 = settings.baseRateBy100();
        }

        @Override
        public void accept(final ShiftSegment segment) {
            int payableMinutes = segment.minutes();

            if (state == null) {
                state = new BatchState();
            }

            state.totalMinutes += payableMinutes;

            // advance on the payment levels until all hours in the shift segment have been paid for
            while (payableMinutes > 0) {

                // number of minutes over the next overtime threshold, if any
                final int excessMinutes = state.overtimeLevel == null ? 0 : Math.max(0, state.totalMinutes - state.overtimeLevel.thresholdMinutes);

                // what should be paid at the current rate
                assert excessMinutes >= 0 : excessMinutes;
                final int paidMinutes = Math.max(0, payableMinutes - excessMinutes);

                // record the payment at the appropriate hourly rate
                state.salaryBy6000 += paidMinutes * (baseRateBy100 + segment.rateBy100() + Math.round((float) baseRateBy100 * (float) state.overtimePercent / (float) 100));

                payableMinutes -= paidMinutes;

                // have we exceeded the current overtime threshold?
                if (excessMinutes > 0) {

                    // use the current overtime rate from now on
                    state.overtimePercent = state.overtimeLevel.percent;

                    // update the current rate and the next threshold
                    state.overtimeLevel = state.overtimePercentIterator.hasNext() ? state.overtimePercentIterator.next() : null;
                }
            }
        }

        @Override
        public void flush() {
            if (state != null) {
                next.accept(Math.round((float) state.salaryBy6000 / (float) 60));
                state = null;
            }
        }

        @Override
        public void close() {
            flush();
        }

        /**
         * Maintains the state of a single batch.
         */
        private final class BatchState {

            // The overtime rate levels to work with.
            private Iterator<OvertimePercent> overtimePercentIterator;

            // The most recent overtime rate; null means no (more) overtime level.
            private OvertimePercent overtimeLevel;

            // The overtime rate for the current overtime level (we start at the regular rates).
            private int overtimePercent = 0;

            // Total number of minutes worked so far today.
            private int totalMinutes = 0;

            // The running sum of the salary along the shift stream, multiplied by 60 (minutes per hour) * 100 (dollar precision).
            private int salaryBy6000;

            BatchState() {
                this.overtimePercentIterator = overtimePercents.iterator();
                this.overtimeLevel = this.overtimePercentIterator.hasNext() ? this.overtimePercentIterator.next() : null;
            }
        }
    }

    /**
     * A shift segment is the total number of minutes in some regular rate period that overlap a list of daily work shifts, along with the applicable regular
     * rate.
     */
    static final class ShiftSegment implements Consumer<WorkShift> {

        // The corresponding regular rate period.
        private final RegularRatePeriod period;

        // The number of minutes in this segment.
        private int minutes;

        /**
         * Creates a new instance for the given regular rate period.
         *
         * @param period        the regular rate period.
         */
        ShiftSegment(final RegularRatePeriod period) {
            this.period = period;
        }

        /**
         * Adds the number of minutes in our regular rate period that overlap with the given shift interval.
         *
         * @param shift the shift interval.
         */
        @Override
        public void accept(final WorkShift shift) {

            // type cast: we are dealing with time in human terms so 32 bits should be enough
            minutes += (int) shift.overlap(period.interval).toMinutes();
        }

        /**
         * Resets the minute count to prepare for the next day's work shifts.
         */
        void reset() {
            minutes = 0;
        }

        /**
         * Returns the the regular hourly rate for this segment.
         *
         * @return a number, always greater than <code>0</code>.
         */
        int rateBy100() {
            return period.rateBy100;
        }

        /**
         * Returns the number of minutes accumulated so far.
         *
         * @return a number of minutes, always greater than or equal to <code>0</code>.
         */
        int minutes() {
            return minutes;
        }
    }
}
