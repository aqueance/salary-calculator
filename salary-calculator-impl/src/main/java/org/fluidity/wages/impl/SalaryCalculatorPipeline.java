package org.fluidity.wages.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.fluidity.composition.Component;
import org.fluidity.wages.Processor;
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

    private final SalaryCalculatorSettings settings;
    private final Consumer<SalaryDetails> consumer;
    private final Collection<WorkShift> shifts = new TreeSet<>();

    SalaryCalculatorPipeline(final SalaryCalculatorSettings settings, final Consumer<SalaryDetails> consumer) {
        this.timeZone = settings.timeZone();
        this.settings = settings;
        this.consumer = consumer;
    }

    @Override
    public void flush() {
        try (final Processor<WorkShift> processor = new ShiftStreamProcessor(settings, consumer)) {
            shifts.forEach(processor);
        }

        shifts.clear();
    }

    @Override
    public void close() {
        flush();
    }

    @Override
    public void accept(final ShiftDetails details) {
        shifts.add(new WorkShift(details, timeZone));
    }

    /**
     * Keeps track of the person whose work shifts are currently being processed.
     */
    private static class PersonDetails {

        private final String personId;
        private final String personName;
        private final LocalDate month;

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
         * Returns the {@link SalaryDetails} object for this person with the given monthly salary.
         *
         * @param salaryBy100 the salary amount to return, multiplied by 100.
         *
         * @return a new {@link SalaryDetails} object; never <code>null</code>.
         */
        SalaryDetails salary(final int salaryBy100) {
            return new SalaryDetails(this.personId, this.personName, this.month, salaryBy100);
        }
    }

    /**
     * Assuming a stream of work shifts sorted by person ID and shift date,
     * an instance of this class maintains a salary object for the current
     * person ID and month, and computes the monthly salary from the
     * corresponding work shift details.
     * <p>
     * A sorted stream allows working on one person at a time rather than
     * using a map to maintain state for all of them at the same time.
     */
    private static final class ShiftStreamProcessor implements Processor<WorkShift> {

        private final SalaryCalculatorSettings settings;
        private final Consumer<SalaryDetails> consumer;

        private Processor<WorkShift> tracker;

        // Keeps track of the person currently being processed.
        private PersonDetails person;

        /**
         * Creates a new instance.
         *
         * @param consumer The consumer to send {@link SalaryDetails} objects to.
         */
        private ShiftStreamProcessor(final SalaryCalculatorSettings settings, final Consumer<SalaryDetails> consumer) {
            this.settings = settings;
            this.consumer = consumer;
        }

        @Override
        public void accept(final WorkShift shift) {
            if (tracker == null || !person.matches(shift)) {
                flush();

                person = new PersonDetails(shift);
                tracker = new DailyShiftTracker(settings, new MonthlySalaryTracker(settings, this::reportSalary));
            }

            tracker.accept(shift);
        }

        private void reportSalary(final int salaryBy100) {
            consumer.accept(person.salary(salaryBy100));
        }

        @Override
        public void flush() {
            if (tracker != null) {
                tracker.close();
            }
        }

        @Override
        public void close() {
            flush();

            tracker = null;
        }
    }

    /**
     * Computes the overlap between the daily shifts of a person and the regular hourly rate periods. For each regular rate period, there is a shift segment
     * that maintains the number of minutes it overlaps with the daily shifts, and the hourly rate for those minutes.
     */
    private static final class DailyShiftSegments implements Processor<WorkShift> {

        private final Processor<ShiftSegment> consumer;
        private final List<ShiftSegment> segments = new ArrayList<>();

        @SuppressWarnings("Convert2streamapi")
        DailyShiftSegments(final SalaryCalculatorSettings settings, final Processor<ShiftSegment> consumer) {
            this.consumer = consumer;

            for (final RegularRatePeriod regularRate : settings.regularRates()) {
                segments.add(new ShiftSegment(regularRate));
            }
        }

        @Override
        public void accept(final WorkShift shift) {
            segments.forEach(segment -> segment.accept(shift));
        }

        @Override
        public void flush() {
            segments.forEach(consumer);
            reset();
        }

        @Override
        public void close() {
            flush();
            consumer.close();
        }

        private void reset() {
            segments.forEach(ShiftSegment::reset);
        }
    }

    /**
     * Instances of this class track the hourly rates of a person during the day, including regular / evening rates and overtime rates. This is done by
     * accepting a list of work shift details sorted by date and producing the monthly salary when done.
     */
    private static final class DailyShiftTracker implements Processor<WorkShift> {

        // the day being processed
        private LocalDate currentDate = null;
        private DailyShiftSegments segments;

        /**
         * Creates a new instance for the given person in the given month.
         *
         * @param consumer the processor for {@link ShiftSegment} objects.
         */
        DailyShiftTracker(final SalaryCalculatorSettings settings, final Processor<ShiftSegment> consumer) {
            this.segments = new DailyShiftSegments(settings, consumer);
        }

        /**
         * Accepts a new work shift of the tracked person in the tracked month.
         * <p>
         * The method adds together the hours of multiple shifts within the same day to check if overtime compensation is due.
         *
         * @param shift the work shift details to accept.
         */
        @Override
        public void accept(final WorkShift shift) {
            final LocalDate shiftDate = shift.date;

            if (currentDate != null && !shiftDate.equals(currentDate)) {
                flush();
                segments.reset();
            }

            currentDate = shiftDate;
            segments.accept(shift);
        }

        @Override
        public void flush() {
            if (segments != null) {
                segments.flush();
            }
        }

        @Override
        public void close() {
            flush();

            segments.close();
        }
    }

    /**
     * Calculates the monthly salary from the consumed daily shifts.
     */
    private static final class MonthlySalaryTracker implements Processor<ShiftSegment> {

        private final Consumer<Integer> salary;

        // The overtime rate levels to work with.
        private final Iterator<Settings.OvertimeRate> overtimeRates;

        // The most recent overtime rate; null means no (more) overtime level.
        private Settings.OvertimeRate overtimeLevel;

        // The overtime rate for the current overtime level (we start at the regular rates).
        private int overtimeRate = 0;

        // Total number of minutes worked so far today.
        private int totalMinutes = 0;

        // The running sum of the salary along the shift stream, multiplied by 60 (minutes per hour) * 100 (dollar precision).
        private int salaryBy6000;

        /**
         * Creates a new instance.
         *
         * @param salary the consumer for the monthly salary.
         */
        MonthlySalaryTracker(final SalaryCalculatorSettings settings, final Consumer<Integer> salary) {
            this.overtimeRates = settings.overtimeRates().iterator();
            this.overtimeLevel = this.overtimeRates.hasNext() ? this.overtimeRates.next() : null;
            this.salary = salary;
        }

        @Override
        public void accept(final ShiftSegment segment) {
            int payableMinutes = segment.minutes();

            totalMinutes += payableMinutes;

            // advance on the payment levels until all hours in the shift segment have been paid for
            while (payableMinutes > 0) {

                // number of minutes over the next overtime threshold, if any
                final int excessMinutes = overtimeLevel == null ? 0 : Math.max(0, totalMinutes - overtimeLevel.thresholdMinutes());

                // what should be paid at the current rate
                assert excessMinutes >= 0 : excessMinutes;
                final int paidMinutes = Math.max(0, payableMinutes - excessMinutes);

                // record the payment at the appropriate hourly rate
                assert overtimeRate == 0 || overtimeRate > segment.rateBy100();
                salaryBy6000 += paidMinutes * Math.max(overtimeRate, segment.rateBy100());

                payableMinutes -= paidMinutes;

                // have we exceeded the current overtime threshold?
                if (excessMinutes > 0) {

                    // use the current overtime rate from now on
                    overtimeRate = overtimeLevel.rateBy100();

                    // update the current rate and the next threshold
                    overtimeLevel = overtimeRates.hasNext() ? overtimeRates.next() : null;
                }
            }
        }

        @Override
        public void flush() {
            salary.accept(Math.round((float) salaryBy6000 / (float) 60));
        }

        @Override
        public void close() {
            flush();
        }
    }

    /**
     * A shift segment is the total number of minutes in some regular rate period that overlap a list of daily work shifts, along with the applicable regular
     * rate.
     */
    private static final class ShiftSegment implements Consumer<WorkShift> {

        // The corresponding regular rate period.
        private final RegularRatePeriod period;

        // The number of minutes in this segment.
        private int minutes;

        /**
         * Creates a new instance for the given regular rate period.
         *
         * @param period the regular rate period.
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
         * @return a number, always greter than <code>0</code>.
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
