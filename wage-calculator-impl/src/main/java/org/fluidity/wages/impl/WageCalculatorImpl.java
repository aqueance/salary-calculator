package org.fluidity.wages.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.fluidity.composition.Component;
import org.fluidity.wages.ShiftDetails;
import org.fluidity.wages.WageCalculator;
import org.fluidity.wages.WageDetails;

@Component
final class WageCalculatorImpl implements WageCalculator {

    private final ZoneId timeZone;
    private final RegularRatePeriod[] regularRates;
    private final OvertimeRate[] overtimeRates;

    WageCalculatorImpl(final WageCalculatorSettings settings) {
        this.timeZone = settings.timeZone();
        this.regularRates = settings.regularRates();
        this.overtimeRates = settings.overtimeRates();
    }

    @Override
    public List<WageDetails> apply(final List<ShiftDetails> shifts) {
        final ShiftStreamProcessor processor = new ShiftStreamProcessor();

        shifts.stream()
                .map(this::shift)
                .sorted()
                .forEach(processor::accept);

        return processor.get();
    }

    private Shift shift(final ShiftDetails details) {
        return new Shift(details, timeZone);
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
    private final class ShiftStreamProcessor implements Supplier<List<WageDetails>> {

        private final List<WageDetails> wages = new ArrayList<>();

        private DailyWageTracker tracker = null;

        /**
         * Receives a work shift stream, one object at a time.
         * <p>
         * Once all shift objects have been processed, the {@link #get get()} method
         * must be invoked.
         *
         * @param shift the work shift details to process.
         */
        void accept(final Shift shift) {
            final String personId = shift.personId;
            final LocalDate month = shift.month();

            if (tracker == null || !tracker.tracking(personId, month)) {
                complete();

                tracker = new DailyWageTracker(personId, month);
            }

            tracker.accept(shift);
        }

        /**
         * Completes and collects the currently tracked salary.
         */
        private void complete() {
            if (tracker != null) {
                wages.add(tracker.get());
            }
        }

        @Override
        public List<WageDetails> get() {
            complete();

            return wages;
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
    private final class DailyWageTracker implements Consumer<Shift>, Supplier<WageDetails> {

        private final String personId;
        private final LocalDate month;

        private final ShiftSegment[] shiftSegments = Stream.of(regularRates).map(ShiftSegment::new).toArray(ShiftSegment[]::new);
        private final MonthlyWage calculator = new MonthlyWage(this::addSalary);

        // the day being processed
        private LocalDate currentDate = null;

        // the running sum of the wage along the shift stream, multiplied by 60 (minutes per hour) * 100 (dollar precision)
        private int wageBy6000;

        /**
         * Creates a new instance for the given person in the given month.
         *
         * @param personId the person ID.
         * @param month    the month.
         */
        DailyWageTracker(final String personId, final LocalDate month) {
            this.personId = personId;
            this.month = month;
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

        /**
         * Produces the monthly wage details for the tracked person in the tracked month.
         *
         * @return a new object; never <code>null</code>.
         */
        @Override
        public WageDetails get() {
            assert currentDate != null : personId;
            calculator.accept(shiftSegments);

            return new WageDetails(personId, month, Math.round((float) wageBy6000 / (float) 60));
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
            final Iterator<OvertimeRate> overtimeRates = Arrays.asList(WageCalculatorImpl.this.overtimeRates).iterator();

            // the most recent overtime rate; null means no (more) overtime level
            OvertimeRate overtimeLevel = overtimeRates.hasNext() ? overtimeRates.next() : null;

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
