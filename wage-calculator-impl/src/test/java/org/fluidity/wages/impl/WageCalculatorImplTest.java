package org.fluidity.wages.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fluidity.foundation.Configuration;
import org.fluidity.testing.Simulator;
import org.fluidity.wages.ShiftDetails;
import org.fluidity.wages.WageCalculator;
import org.fluidity.wages.WageDetails;

import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

public final class WageCalculatorImplTest extends Simulator {

    private final WageCalculator.Settings settings = dependencies().normal(WageCalculator.Settings.class);

    private final Configuration<WageCalculator.Settings> configuration = new Configuration<WageCalculator.Settings>() {
        @Override
        public WageCalculator.Settings settings() {
            return settings;
        }

        @Override
        public <R> R query(final Query<R, WageCalculator.Settings> query) {
            throw new UnsupportedOperationException();
        }
    };

    // TODO
    private void expectConfiguration(final String timeZone, final List<WageCalculator.RegularRate> regular, final List<WageCalculator.OvertimeRate> overtime) {
        EasyMock.expect(settings.timeZone()).andReturn(timeZone).anyTimes();
        EasyMock.expect(settings.regularRates()).andReturn(regular).anyTimes();
        EasyMock.expect(settings.overtimeRates()).andReturn(overtime).anyTimes();
    }

    private LocalDateTime createDate(final int year, final int month, final int day, final int hour, final int minute) {
        return LocalDateTime.of(year, month, day, hour, minute);
    }

    @Test
    public void acceptsEmptyList() throws Exception {
        expectConfiguration("Europe/Helsinki", Collections.singletonList(regularRate(100, 0)), Collections.emptyList());

        final WageCalculator subject = verify(() -> new WageCalculatorImpl(configuration));

        verify(() -> {
            final List<WageDetails> wages = subject.calculate(Collections.emptyList());
            Assert.assertNotNull(wages);
            Assert.assertTrue(wages.isEmpty());
        });
    }

    @Test
    public void computesWagesForOneHourShift() throws Exception {
        final String zoneName = "Europe/Helsinki";
        final ZoneId timeZone = ZoneId.of(zoneName);

        final int regularRate = 100;
        final int eveningRate = 150;

        final int overtimeSchedule1Rate = 200;
        final int overtimeSchedule2Rate = 300;

        expectConfiguration(zoneName,

                            // regular hours $1.00 from 10:00 to 15:00
                            // evening hours $1.50 from 15:00 to 10:00
                            Arrays.asList(regularRate(regularRate, 10 * 60),
                                          regularRate(eveningRate, 15 * 60)),

                            // overtime compensation:
                            //  $2.00 from 4 hours
                            //  $3.00 from 6 hours
                            Arrays.asList(overtimeRate(overtimeSchedule1Rate, 4 * 60),
                                          overtimeRate(overtimeSchedule2Rate, 6 * 60))
        );

        final WageCalculator subject = verify(() -> new WageCalculatorImpl(configuration));

        final int year = 2000;
        final Month month = Month.JANUARY;
        final String personId = "1";

        final List<ShiftDetails> shifts = Collections.singletonList(
                new ShiftDetails(personId,
                                 LocalDate.of(year, month, 1),
                                 LocalTime.of(12, 0),
                                 LocalTime.of(13, 0))
        );

        verify(() -> {
            final List<WageDetails> wages = subject.calculate(shifts);
            Assert.assertNotNull(wages);
            Assert.assertEquals(wages.size(), 1);

            final WageDetails details = wages.get(0);
            Assert.assertEquals(details.personId(), personId);
            Assert.assertEquals(details.salaryBy100(), regularRate);    // 1 hour on regular rate

            // verify the month
            Assert.assertEquals(details.date().getYear(), year);
            Assert.assertEquals(details.date().getMonth(), month);
            Assert.assertEquals(details.date().getDayOfMonth(), 1);
        });
    }

    private static WageCalculator.RegularRate regularRate(final int rate, final int fromMinute) {
        return new WageCalculator.RegularRate() {
            @Override
            public int rateBy100() {
                return rate;
            }

            @Override
            public int fromMinute() {
                return fromMinute;
            }
        };
    }

    private static WageCalculator.OvertimeRate overtimeRate(final int rate, final int fromMinutes) {
        return new WageCalculator.OvertimeRate() {
            @Override
            public int thresholdMinutes() {
                return fromMinutes;
            }

            @Override
            public int rateBy100() {
                return rate;
            }
        };
    }
}
