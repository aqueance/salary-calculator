package org.fluidity.wages.impl;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.fluidity.composition.Component;
import org.fluidity.foundation.Configuration;
import org.fluidity.wages.SalaryCalculator;

/**
 * Turns the settings in a {@link SalaryCalculator.Settings} object into a format that is directly palatable to the {@link
 * SalaryCalculatorPipeline}.
 */
@Component
final class SalaryCalculatorSettingsImpl implements SalaryCalculatorSettings {

    private final ZoneId timeZone;
    private final List<RegularRatePeriod> regularRates;
    private final List<SalaryCalculator.Settings.OvertimeRate> overtimeRates;

    SalaryCalculatorSettingsImpl(final Configuration<SalaryCalculator.Settings> configuration) {
        final SalaryCalculator.Settings settings = configuration.settings();

        this.timeZone = ZoneId.of(settings.timeZone());

        final List<SalaryCalculator.Settings.RegularRate> regularRates = settings.regularRates();
        assert regularRates != null;
        this.regularRates = Collections.unmodifiableList(regularRatePeriods(regularRates));

        final int highestRegularRate = regularRates.stream().mapToInt(SalaryCalculator.Settings.RegularRate::rateBy100).max().orElse(0);

        final List<SalaryCalculator.Settings.OvertimeRate> overtimeRates = settings.overtimeRates();
        assert overtimeRates != null;

        validateOvertimeRates(highestRegularRate, overtimeRates);

        this.overtimeRates = Collections.unmodifiableList(overtimeRates);
    }

    private List<RegularRatePeriod> regularRatePeriods(List<SalaryCalculator.Settings.RegularRate> rates) {
        if (rates.isEmpty()) {
            throw new IllegalArgumentException("No regular rates specified");
        }

        SalaryCalculator.Settings.RegularRate lastRate = rates.get(0);

        // make sure the first period starts at midnight
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
            final SalaryCalculator.Settings.RegularRate nextRate = rates.get(i);
            periods.add(regularRatePeriod(lastRate, nextRate));
            lastRate = nextRate;
        }

        periods.add(regularRatePeriod(lastRate, rates.get(0)));

        return periods;
    }

    /**
     * TODO
     *
     * @param lastRate TODO
     * @param rates    TODO
     *
     * @return TODO
     */
    private int validateOvertimeRates(int lastRate, final List<SalaryCalculator.Settings.OvertimeRate> rates) {
        int lastThreshold = 0;

        for (final SalaryCalculator.Settings.OvertimeRate rate : rates) {
            final int currentRate = rate.rateBy100();
            final int currentThreshold = rate.thresholdMinutes();

            if (currentRate < lastRate) {
                throw new IllegalArgumentException(String.format("Overtime rate %d is less than previous rate %d", currentRate, lastRate));
            } else if (currentThreshold < lastThreshold) {
                throw new IllegalArgumentException(String.format("Overtime threshold minutes %d is less than previous threshold %d",
                                                                 currentThreshold,
                                                                 lastThreshold));
            }

            lastRate = currentRate;
        }

        return lastRate;
    }

    private SalaryCalculator.Settings.RegularRate regularRateFromMidnight(final int rate) {
        return new SalaryCalculator.Settings.RegularRate() {

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

    private RegularRatePeriod regularRatePeriod(final SalaryCalculator.Settings.RegularRate currentRate, final SalaryCalculator.Settings.RegularRate nextRate) {
        return new RegularRatePeriod(currentRate.rateBy100(), localTime(currentRate.fromMinute()), localTime(nextRate.fromMinute()));
    }

    private static LocalTime localTime(final int minutes) {
        return LocalTime.of(minutes / 60, minutes % 60);
    }

    @Override
    public ZoneId timeZone() {
        return timeZone;
    }

    @Override
    public List<RegularRatePeriod> regularRates() {
        return regularRates;
    }

    @Override
    public List<SalaryCalculator.Settings.OvertimeRate> overtimeRates() {
        return overtimeRates;
    }
}
