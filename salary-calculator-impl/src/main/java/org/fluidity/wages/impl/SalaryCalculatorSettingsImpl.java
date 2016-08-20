/*
 * Copyright (c) 2016 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.wages.impl;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private final int baseRateBy100;
    private final List<RegularRatePeriod> regularRates;
    private final List<OvertimePercent> overtimePercents;

    SalaryCalculatorSettingsImpl(final Configuration<SalaryCalculator.Settings> configuration) {
        final SalaryCalculator.Settings settings = configuration.settings();

        this.timeZone = ZoneId.of(settings.timeZone());
        baseRateBy100 = settings.baseRateBy100();

        final List<SalaryCalculator.Settings.RegularRate> regularRates = settings.regularRates();
        assert regularRates != null;
        this.regularRates = Collections.unmodifiableList(regularRatePeriods(regularRates));

        final List<SalaryCalculator.Settings.OvertimeLevel> overtimeLevels = settings.overtimeLevels();
        assert overtimeLevels != null;

        this.overtimePercents = Collections.unmodifiableList(overtimeLevels.stream()
                                                                     .map(rate -> new OvertimePercent(rate.percent(),
                                                                                                      rate.thresholdHours(),
                                                                                                      rate.thresholdMinutes()))
                                                                     .collect(Collectors.toList()));
    }

    private List<RegularRatePeriod> regularRatePeriods(List<SalaryCalculator.Settings.RegularRate> rates) {
        if (rates.isEmpty()) {
            throw new IllegalArgumentException("No regular rates specified");
        }

        SalaryCalculator.Settings.RegularRate lastRate = rates.get(0);

        // make sure the first period starts at midnight
        if (lastRate.fromHour() + lastRate.fromMinute() > 0) {
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

    private SalaryCalculator.Settings.RegularRate regularRateFromMidnight(final int rate) {
        return new SalaryCalculator.Settings.RegularRate() {

            @Override
            public int rateBy100() {
                return rate;
            }

            @Override
            public int fromHour() {
                return 0;
            }

            @Override
            public int fromMinute() {
                return 0;
            }
        };
    }

    private RegularRatePeriod regularRatePeriod(final SalaryCalculator.Settings.RegularRate currentRate, final SalaryCalculator.Settings.RegularRate nextRate) {
        return new RegularRatePeriod(currentRate.rateBy100(), fromTime(currentRate), fromTime(nextRate));
    }

    private LocalTime fromTime(final SalaryCalculator.Settings.RegularRate rate) {
        return LocalTime.of(rate.fromHour(), rate.fromMinute());
    }

    @Override
    public ZoneId timeZone() {
        return timeZone;
    }

    @Override
    public int baseRateBy100() {
        return baseRateBy100;
    }

    @Override
    public List<RegularRatePeriod> regularRates() {
        return regularRates;
    }

    @Override
    public List<OvertimePercent> overtimeLevels() {
        return overtimePercents;
    }
}
