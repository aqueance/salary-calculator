package org.fluidity.wages;

import java.util.List;
import java.util.function.Consumer;

import org.fluidity.foundation.Configuration;

/**
 * A pipeline that takes a stream of work shift details and emits a stream of monthly salaries.
 * <p>
 * Create an instance using {@link WageCalculator.Factory#create(Consumer)}, and pass the stream of {@link ShiftDetails} objects to it. When done, make sure
 * that {@link AutoCloseable#close()} is invoked on the instance. The stream of {@link WageDetails} objects will be sent, in an order sorted by person name
 * and date, to the consumer the instance was created with.
 * <p>
 * Example:
 * <pre>
 *     SalaryCalculator.Factory factory = &hellip;;
 *     &hellip;
 *     final List&lt;ShiftDetails&gt; shifts = &hellip;;
 *
 *     try (final Processor&lt;ShiftDetails&gt; calculator = factory.create(System.out::println)) {
 *         shifts.forEach(calculator);
 *     }
 * </pre>
 * The implementation of the {@link WageCalculator.Factory} interface is a
 * <a href="https://github.com/aqueance/fluid-tools/wiki/User%20Guide%20-%20Overview#components">dependency injected component</a>.
 */
public interface WageCalculator extends Processor<ShiftDetails> {

    // no specific methods other than those inherited from Processor.

    /**
     * Creates new {@link WageCalculator} instances.
     */
    interface Factory {

        /**
         * Creates a new {@link WageCalculator}.
         *
         * @param consumer the consumer for the {@link WageDetails} stream.
         *
         * @return a new instance; never <code>null</code>.
         */
        WageCalculator create(Consumer<WageDetails> consumer);
    }

    /**
     * Configures the salary calculator with the time zone and the hourly rate levels.
     */
    interface Settings {

        /**
         * Specifies the time zone in which work shift dates are interpreted.
         *
         * @return a time zone code to pass to {@link java.time.ZoneId#of(String)}.
         */
        @Configuration.Property(key = "time.zone")
        String timeZone();

        /**
         * The list of regular hourly rates during a day.
         *
         * @return a list; never <code>null</code> or empty.
         */
        @Configuration.Property(key = "regular.rates")
        List<RegularRate> regularRates();

        /**
         * The list of overtime rates within a day.
         *
         * @return a list; never <code>null</code> or empty.
         */
        @Configuration.Property(key = "overtime.rates")
        List<OvertimeRate> overtimeRates();

        /**
         * Represents a regular hourly rate level. The regular rate applies during a specific interval, the first minute of which is returned by {@link
         * #fromMinute()}. The first minute when this rate does not apply is given in the next instance in the list returned by {@link
         * Settings#regularRates()}.
         */
        interface RegularRate {

            /**
             * Specifies the first minute of the day at which this hourly rate level applies.
             *
             * @return a number of minutes; equal to or greater than <code>0</code>.
             */
            @Configuration.Property(key = "from.minute")
            int fromMinute();

            /**
             * Specifies the hourly rate multiplied by 100.
             *
             * @return an integer; greater than <code>0</code>.
             */
            @Configuration.Property(key = "rate.by.100")
            int rateBy100();
        }

        /**
         * Represents an overtime rate level.
         */
        interface OvertimeRate {

            /**
             * Specifies the threshold in minutes over which this overtime rate applies.
             *
             * @return a number of minutes; greater than <code>0</code>.
             */
            @Configuration.Property(key = "threshold.minutes")
            int thresholdMinutes();

            /**
             * Specifies the hourly rate multiplied by 100.
             *
             * @return an integer; greater than <code>0</code>.
             */
            @Configuration.Property(key = "rate.by.100")
            int rateBy100();
        }
    }
}
