package org.fluidity.wages.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.fluidity.composition.Component;
import org.fluidity.wages.ShiftDetails;
import org.fluidity.wages.WageCalculator;
import org.fluidity.wages.WageDetails;

@Component
final class WageCalculatorFactory implements WageCalculator.Factory {

    private final ZoneId timeZone;
    private final List<RegularRatePeriod> regularRates;
    private final List<WageCalculator.Settings.OvertimeRate> overtimeRates;

    WageCalculatorFactory(final WageCalculatorSettings settings) {
        this.timeZone = settings.timeZone();
        this.regularRates = Arrays.asList(settings.regularRates());
        this.overtimeRates = Arrays.asList(settings.overtimeRates());
    }

    @Override
    public WageCalculator create(final Consumer<WageDetails> consumer) {
        return new WageCalculatorPipeline(consumer);
    }

    /**
     * Implements the wage calculator as a pipeline of auto-closeable consumers. The {@link AutoCloseable#close()} method flushes the pipeline.
     */
    private final class WageCalculatorPipeline implements WageCalculator {

        private final Consumer<WageDetails> consumer;
        private final Collection<Shift> shifts = new TreeSet<>();

        private WageCalculatorPipeline(final Consumer<WageDetails> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void close() {
            try (final ShiftStreamProcessor processor = new ShiftStreamProcessor(consumer)) {
                shifts.forEach(processor);
            }
        }

        @Override
        public void accept(final ShiftDetails details) {
            shifts.add(new Shift(details, timeZone));
        }
    }

    /**
     * Assuming a stream of work shifts sorted by person ID and shift date,
     * an instance of this class maintains a wage object for the current
     * person ID and month, and computes the monthly wages from the
     * corresponding work shift details.
     * <p>
     * A sorted stream allows maintaining a single wage object at a time
     * rather than using a map to maintain all of them at the same time.
     */
    private final class ShiftStreamProcessor implements Consumer<Shift>, AutoCloseable {

        private final Consumer<WageDetails> consumer;

        private DailyWageTracker tracker = null;

        private ShiftStreamProcessor(final Consumer<WageDetails> consumer) {
            this.consumer = consumer;
        }

        /**
         * Receives a work shift stream, one object at a time.
         *
         * @param shift the work shift details to process.
         */
        public void accept(final Shift shift) {
            final String personId = shift.personId;
            final LocalDate month = shift.month();

            if (tracker == null || !tracker.tracking(personId, month)) {
                flush();

                tracker = new DailyWageTracker(personId, shift.personName, month, consumer);
            }

            tracker.accept(shift);
        }

        /**
         * Completes and flushes the currently tracked salary.
         */
        private void flush() {
            if (tracker != null) {
                tracker.close();
                tracker =  null;
            }
        }

        @Override
        public void close() {
            flush();
        }
    }

    /**
     * A shift segment is a number of minutes that overlap a regular rate period, along with the applicable regular rate.
     */
    private static final class ShiftSegment {

        final int rateBy100;
        int minutes;

        private final LocalTimeInterval interval;

        /**
         * Creates a new instance for the given regular rate period.
         *
         * @param period the regular rate period.
         */
        ShiftSegment(final RegularRatePeriod period) {
            this.rateBy100 = period.rateBy100;
            this.interval = period.interval;
        }

        /**
         * Adds the minutes in the period overlapping with the given shift interval.
         *
         * @param shift the shift interval.
         */
        void addOverlap(final Shift shift) {

            // type cast: we are dealing with time in human terms so 32 bits should be enough
            this.minutes += (int) shift.overlap(this.interval).toMinutes();
        }

        /**
         * Resets the minute count to prepare for the next day's work shifts.
         */
        void reset() {
            this.minutes = 0;
        }
    }

    /**
     * Instances of this class track the hourly rates of person during the day, including regular / evening rates and overtime rates. This is done by accepting
     * a list of work shift details sorted by date and producing the monthly wage when done.
     */
    private final class DailyWageTracker implements Consumer<Shift>, AutoCloseable {

        private final String personId;
        private final String personName;
        private final LocalDate month;

        private final ShiftSegment[] shiftSegments = regularRates.stream().map(ShiftSegment::new).toArray(ShiftSegment[]::new);
        private final MonthlyWage calculator = new MonthlyWage(this::addSalary);
        private final Consumer<WageDetails> consumer;

        // the day being processed
        private LocalDate currentDate = null;

        // the running sum of the wage along the shift stream, multiplied by 60 (minutes per hour) * 100 (dollar precision)
        private int wageBy6000;

        /**
         * Creates a new instance for the given person in the given month.
         *
         * @param personId   the person ID.
         * @param personName the person's name.
         * @param month      the month.
         * @param consumer   the consumer to send {@link WageDetails} objects to.
         */
        DailyWageTracker(final String personId, final String personName, final LocalDate month, final Consumer<WageDetails> consumer) {
            this.personId = personId;
            this.personName = personName;
            this.month = month;
            this.consumer = consumer;
        }

        /**
         * Checks if this instances tracks the shifts of the given person in the given month.
         *
         * @param personId the ID of the person to check.
         * @param month    the month to check.
         *
         * @return <code>true</code> if this instances indeed tracks the given details, <code>false</code> otherwise.
         */
        boolean tracking(final String personId, final LocalDate month) {
            return this.personId.equals(personId) && this.month.equals(month);
        }

        /**
         * Adds the hourly rate for the given number of minutes to the tracked person's monthly wage.
         *
         * @param minutes   the number of minutes to add.
         * @param rateBy100 the hourly rate for the given number of minutes.
         */
        private void addSalary(final int minutes, final int rateBy100) {
            wageBy6000 += minutes * rateBy100;
        }

        /**
         * Accepts a new work shift of the tracked person in the tracked month.
         * <p>
         * The method adds together the hours of multiple shifts within the same day to check if overtime compensation is due.
         *
         * @param shift the work shift details to accept.
         */
        @Override
        public void accept(final Shift shift) {
            final LocalDate shiftDate = shift.date;

            if (currentDate != null && !shiftDate.equals(currentDate)) {
                calculator.accept(shiftSegments);
                resetShiftSegments();
            }

            currentDate = shiftDate;

            updateShiftSegments(shift);
        }

        /**
         * Calculates the overlap between the regular rate intervals and that of the given work shift and records the number of minutes in each rate level.
         *
         * @param shift the work shift to process.
         */
        private void updateShiftSegments(final Shift shift) {
            for (final ShiftSegment segment : shiftSegments) {
                segment.addOverlap(shift);
            }
        }

        /**
         * Resets the recorded work minutes for each regular rate level, so that subsequent invocations of {@link #updateShiftSegments(Shift)} have a baseline.
         */
        private void resetShiftSegments() {
            for (final ShiftSegment segment : shiftSegments) {
                segment.reset();
            }
        }

        @Override
        public void close() {
            assert currentDate != null : personId;
            calculator.accept(shiftSegments);

            consumer.accept(new WageDetails(personId, personName, month, Math.round((float) wageBy6000 / (float) 60)));
        }
    }

    /**
     * A functional interface to accept a number of minutes with some hourly rate.
     */
    @FunctionalInterface
    private interface PaymentConsumer {

        /**
         * Accepts a number of minutes remunerated at a given hourly rate.
         *
         * @param minutes   the number of minutes.
         * @param rateBy100 the hourly rate multiplied by 100 (precision for dollar amounts).
         */
        void accept(int minutes, int rateBy100);
    }

    /**
     * Calculates the monthly wage from the consumed daily shifts.
     */
    private final class MonthlyWage implements Consumer<ShiftSegment[]> {

        private final PaymentConsumer wages;

        MonthlyWage(final PaymentConsumer wages) {
            this.wages = wages;
        }

        /**
         * Accepts a list of shift segments that represent a day's work.
         *
         * @param segments the array of shift segments.
         */
        public void accept(final ShiftSegment[] segments) {
            final Iterator<WageCalculator.Settings.OvertimeRate> overtimeRates = WageCalculatorFactory.this.overtimeRates.iterator();

            // the most recent overtime rate; null means no (more) overtime level
            WageCalculator.Settings.OvertimeRate overtimeLevel = overtimeRates.hasNext() ? overtimeRates.next() : null;

            // the threshold for the next overtime rate: 0 means no (more) overtime level
            int overtimeThreshold = overtimeLevel == null ? 0 : overtimeLevel.thresholdMinutes();

            // the overtime rate for the current overtime level
            int overtimeRate = 0;

            // total number of minutes so far today
            int shiftMinutes = 0;

            // keep track of what hourly rate to apply to the various shift segments
            for (final ShiftSegment segment : segments) {
                final int segmentMinutes = segment.minutes;

                if (segmentMinutes > 0) {
                    shiftMinutes += segmentMinutes;

                    // number of minutes over the next overtime threshold
                    final int excessMinutes = overtimeThreshold == 0 ? 0 : shiftMinutes - overtimeThreshold;

                    // the hourly rate to apply to the minutes under the next overtime threshold
                    final int segmentRate = overtimeRate > 0 ? overtimeRate : segment.rateBy100;

                    if (excessMinutes > 0) {
                        if (overtimeRates.hasNext()) {
                            overtimeRate = overtimeLevel.rateBy100();
                            overtimeLevel = overtimeRates.next();
                            overtimeThreshold = overtimeLevel.thresholdMinutes();
                        } else {
                            overtimeThreshold = 0;      // no more overtime levels
                        }

                        wages.accept(segmentMinutes - excessMinutes, segmentRate);
                        wages.accept(excessMinutes, overtimeRate);
                    } else {
                        wages.accept(segmentMinutes, segmentRate);
                    }
                }
            }
        }
    }
}
