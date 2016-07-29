package org.fluidity.wages;

import java.util.List;
import java.util.function.Function;

import org.fluidity.foundation.Configuration;

/**
 * Computes wages from a list of shift details.
 */
public interface WageCalculator extends Function<List<ShiftDetails>, List<WageDetails>> {

    /**
     * TODO
     *
     * @param shifts TODO.
     *
     * @return TODO.
     */
    List<WageDetails> apply(List<ShiftDetails> shifts);

    /**
     * TODO
     */
    interface Settings {

        @Configuration.Property(key = "timelzone")
        String timeZone();

        @Configuration.Property(key = "regular.rates")
        List<RegularRate> regularRates();

        @Configuration.Property(key = "overtime.rates")
        List<OvertimeRate> overtimeRates();
    }

    /**
     * TODO
     */
    interface RegularRate {

        @Configuration.Property(key = "from.rate.by.100")
        int rateBy100();

        @Configuration.Property(key = "from.minute")
        int fromMinute();
    }

    /**
     * TODO
     */
    interface OvertimeRate {

        @Configuration.Property(key = "from.minutes")
        int thresholdMinutes();

        @Configuration.Property(key = "rate.by.100")
        int rateBy100();
    }
}
