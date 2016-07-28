package org.fluidity.wages.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.fluidity.composition.Component;
import org.fluidity.foundation.Configuration;
import org.fluidity.foundation.Lists;
import org.fluidity.wages.ShiftDetails;
import org.fluidity.wages.WageCalculator;
import org.fluidity.wages.WageDetails;

@Component
final class WageCalculatorImpl implements WageCalculator {

    private final ZoneId timeZone;
    private final RegularRatePeriod[] regularRates;
    private final OvertimeRate[] overtimeRates;

    /*
     * This processor waits for all shifts of a person to arrive
     * before processing their shifts, thus processing the last
     * shift has to be followed by a call to complete() on the
     * returned wage object.
     */
    private final Function<Shift, WageCalculationState> process = new Function<Shift, WageCalculationState>() {

        // the current wage (assumes shifts are sorted by person ID)
        private WageCalculationState wage = null;

        @Override
        public WageCalculationState apply(final Shift shift) {
            final String personId = shift.personId;

            final LocalDate date = shift.date;
            final ZonedDateTime month = ZonedDateTime.of(LocalDate.of(date.getYear(), date.getMonth(), 1), LocalTime.MIDNIGHT, timeZone);

            if (wage == null || !wage.personId.equals(personId) || !wage.date.equals(month)) {
                wage = new WageCalculationState(personId, month);
            }

            // accept the next shift
            // assumes shifts are sorted by date and start time
            wage.accept(shift);

            // return the wage, the same instance for all shifts of the same person within a month
            return wage;
        }
    };

    // TODO: move to configuration processor
    WageCalculatorImpl(final Configuration<Settings> configuration) {
        final WageCalculator.Settings settings = configuration.settings();

        this.timeZone = ZoneId.of(settings.timeZone());

        final List<RegularRate> regularRates = settings.regularRates();
        this.regularRates = Lists.asArray(RegularRatePeriod.class, regularRatePeriods(regularRates));

        final int highestRegularRate = regularRates.stream().mapToInt(RegularRate::rateBy100).max().orElse(0);

        final List<OvertimeRate> overtimeRates = settings.overtimeRates();
        validateOvertimeRates(highestRegularRate, overtimeRates);

        this.overtimeRates = Lists.asArray(OvertimeRate.class, overtimeRates);
    }

    // TODO: move to configuration processor
    private List<RegularRatePeriod> regularRatePeriods(List<RegularRate> rates) {
        if (rates.isEmpty()) {
            throw new IllegalArgumentException("No regular rates specified");
        }

        RegularRate lastRate = rates.get(0);

        // make sure we the first period starts at midnight
        if (lastRate.fromMinute() > 0) {
            final int periodCount = rates.size();

            // the last hourly rate starting at midnight
            lastRate = regularRateFromMidnight(rates.get(periodCount - 1).rateBy100());

            // we are going to modify the list
            rates = new ArrayList<>(rates);

            if (periodCount == 1) {
                rates.set(0, lastRate);
            } else {
                assert rates.size() > 1;        // verified above that it is not empty
                rates.add(0, lastRate);
            }
        }

        final List<RegularRatePeriod> periods = new ArrayList<>(rates.size());

        for (int i = 1, ii = rates.size(); i < ii; i++) {
            final RegularRate nextRate = rates.get(i);
            periods.add(new RegularRatePeriod(lastRate, nextRate));
            lastRate = nextRate;
        }

        periods.add(new RegularRatePeriod(lastRate, rates.get(0)));

        return periods;
    }

    // TODO: move to configuration processor
    private static final class RegularRatePeriod {

        final int rateBy100;
        final LocalTimeInterval interval;

        RegularRatePeriod(final RegularRate currentRate, final RegularRate nextRate) {
            this.interval = new LocalTimeInterval(localTime(currentRate.fromMinute()), localTime(nextRate.fromMinute()));
            this.rateBy100 = currentRate.rateBy100();
        }

        private static LocalTime localTime(final int minutes) {
            return LocalTime.of(minutes / 60, minutes % 60);
        }
    }

    /**
     * TODO
     * @param lastRate    TODO
     * @param rates       TODO
     * @return TODO
     */
    // TODO: move to configuration processor
    private int validateOvertimeRates(int lastRate, final List<OvertimeRate> rates) {
        int lastThreshold = 0;

        for (final OvertimeRate rate : rates) {
            final int currentRate = rate.rateBy100();
            final int currentThreshold = rate.thresholdMinutes();

            if (currentRate < lastRate) {
                throw new IllegalArgumentException(String.format("Overtime rate %d is less than previous rate %d", currentRate, lastRate));
            } else if (currentThreshold < lastThreshold) {
                throw new IllegalArgumentException(String.format("Overtime threshold minutes %d is less than previous threshold %d", currentThreshold, lastThreshold));
            }

            lastRate = currentRate;
        }

        return lastRate;
    }

    // TODO: move to configuration processor
    private RegularRate regularRateFromMidnight(final int rate) {
        return new RegularRate() {

            @Override
            public int rateBy100() {
                return rate;
            }

            @Override
            public int fromMinute() {
                return 0;
            }
        };
    }

    @Override
    public List<WageDetails> calculate(final List<ShiftDetails> shifts) {
        return shifts
                .stream()                               // process each shift in turn
                .map(this::shift)                       // convert to internal format
                .sorted()                               // but sort them to avoid having to keep excessive amount of state around
                .map(process)                           // process the shifts (assumes shifts are sorted by person, date, and time)
                .distinct()                             // collapse lists of identical wage objects into single instances
                .map(WageCalculationState::complete)    // process the last shifts
                .collect(Collectors.toList());          // return a list
    }

    private Shift shift(final ShiftDetails details) {
        return new Shift(details, timeZone);
    }

    private final class ShiftCalculationState {

        private OvertimeRate overtimeRate;  // null means no (more) overtime schedule
        private int overtimeThreshold;      // 0 means no (more) overtime schedule
        private int overtimeRateIndex;      // the current index in the overtime schedule array

        private int totalMinutes;           // total number of minute so far today
        private boolean overtime;           // indicates that the total working time exceeded the overtime threshold for today

        private int wageBy6000;             // the running sum of the wage, multiplied by 60 (minutes per hour) * 100 (dollar precision)

        /**
         * Resets the state to prepare to process another day's shifts.
         */
        private void initialize() {
            overtimeRateIndex = 0;
            overtimeRate = overtimeRates.length == 0 ? null : overtimeRates[overtimeRateIndex];
            overtimeThreshold = overtimeRate == null ? 0 : overtimeRate.thresholdMinutes();

            totalMinutes = 0;
            overtime = false;
        }

        /**
         * TODO
         *
         * @param shiftMinutes TODO
         */
        void process(final int[] shiftMinutes) {
            initialize();

            for (int i = 0, ii = shiftMinutes.length; i < ii; ++i) {
                final int currentMinutes = shiftMinutes[i];

                if (currentMinutes > 0) {
                    totalMinutes += currentMinutes;

                    final int overtimeMinutes = overtimeThreshold == 0 ? 0 : totalMinutes - overtimeThreshold;
                    final int currentRate = overtime ? overtimeRate.rateBy100() : regularRates[i].rateBy100;

                    if (overtimeMinutes > 0) {
                        overtime = true;

                        collectRate(currentMinutes - overtimeMinutes, currentRate);
                        collectRate(overtimeMinutes, overtimeRate.rateBy100());

                        if (overtimeRates.length < ++overtimeRateIndex) {
                            overtimeRate = overtimeRates[overtimeRateIndex];
                            overtimeThreshold = overtimeRate.thresholdMinutes();
                        }
                    } else {
                        collectRate(currentMinutes, currentRate);
                    }
                }
            }
        }

        /**
         * TODO
         * @param minutes TODO
         * @param rate TODO
         */
        private void collectRate(final float minutes, final int rate) {
            wageBy6000 += minutes * rate;
        }
    }

    /**
     * TODO
     */
    private final class WageCalculationState {

        private final String personId;
        private final ZonedDateTime date;

        private final int[] shiftMinutes = new int[regularRates.length];
        private LocalDate currentDate = null;

        private final ShiftCalculationState shiftState = new ShiftCalculationState();

        WageCalculationState(final String personId, final ZonedDateTime date) {
            this.personId = personId;
            this.date = date;
        }

        /**
         * TODO
         * @param shift TODO
         */
        void accept(final Shift shift) {
            final LocalDate shiftDate = shift.date;

            if (currentDate != null && !shiftDate.equals(currentDate)) {
                shiftState.process(shiftMinutes);
                resetShiftMinutes();
            }

            currentDate = shiftDate;

            updateShiftMinutes(shift);
        }

        private void updateShiftMinutes(final Shift shift) {
            for (int i = 0, ii = regularRates.length; i < ii; ++i) {

                // type cast: we are dealing with time in human terms, integer should have enough precision
                shiftMinutes[i] = (int) shift.overlap(regularRates[i].interval).toMinutes();
            }
        }

        private void resetShiftMinutes() {
            for (int i = 0, ii = shiftMinutes.length; i < ii; ++i) {
                shiftMinutes[i] = 0;
            }
        }

        /**
         * TODO
         * @return
         */
        WageDetails complete() {
            assert currentDate != null : personId;
            shiftState.process(shiftMinutes);

            return new Wage(personId, date, Math.round((float) shiftState.wageBy6000 / (float) 60));
        }
    }
}
