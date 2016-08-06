package org.fluidity.wages;

import java.util.List;
import java.util.function.Consumer;

import org.fluidity.foundation.Configuration;

/**
 * A pipeline that takes a stream of work shift details and emits a stream of monthly salaries.
 * <p>
 * Create an instance using {@link SalaryCalculator.Factory#create(Consumer)}, and pass the stream of {@link ShiftDetails} objects to it. When done, make sure
 * that {@link AutoCloseable#close()} is invoked on the instance. The stream of {@link SalaryDetails} objects will be sent, in an order sorted by person name
 * and date, to the consumer the instance was created with.
 * <p>
 * Example:
 * <pre>
 *     SalaryCalculator.Factory factory = &hellip;;
 *     &hellip;
 *     final List&lt;ShiftDetails&gt; shifts = &hellip;;
 *
 *     try (final BatchProcessor&lt;ShiftDetails&gt; calculator = factory.create(System.out::println)) {
 *         shifts.forEach(calculator);
 *     }
 * </pre>
 * The implementation of the {@link SalaryCalculator.Factory} interface is a
 * <a href="https://github.com/aqueance/fluid-tools/wiki/User%20Guide%20-%20Overview#components">dependency injected component</a>.
 */
public interface SalaryCalculator extends BatchProcessor<ShiftDetails> {

    // no specific methods other than those inherited from BatchProcessor.

    /**
     * Creates new {@link SalaryCalculator} instances.
     */
    interface Factory {

        /**
         * Creates a new {@link SalaryCalculator}.
         *
         * @param consumer the consumer for the {@link SalaryDetails} stream.
         *
         * @return a new instance; never <code>null</code>.
         */
        SalaryCalculator create(Consumer<SalaryDetails> consumer);
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
         * Specifies the base hourly rate multiplies by 100 (the dollar amount precision) to which other compensations are added.
         *
         * @return a number; greater than <code>0</code>.
         */
        @Configuration.Property(key = "base.rate")
        int baseRateBy100();

        /**
         * The list of regular hourly rates during a day.
         *
         * @return a list; never <code>null</code> or empty.
         */
        @Configuration.Property(key = "regular.rates", ids = "list")
        List<RegularRate> regularRates();

        /**
         * The list of overtime rates within a day.
         *
         * @return a list; never <code>null</code> or empty.
         */
        @Configuration.Property(key = "overtime.levels", ids = "list")
        List<OvertimeLevel> overtimeLevels();

        /**
         * Represents a regular hourly rate level. The regular rate applies during a specific interval, the first hour and minute of which are returned by
         * {@link #fromHour()} and {@link #fromMinute()}, respectively. The first minute when this rate does not apply is given in the next instance in the list
         * returned by {@link Settings#regularRates()}.
         */
        interface RegularRate {

            /**
             * Specifies the hour of the day at which this hourly rate level applies.
             *
             * @return a number of minutes; equal to or greater than <code>0</code>.
             */
            @Configuration.Property(key = "from.hour")
            int fromHour();

            /**
             * Specifies the minute in the hour of the day returned by {@link #fromHour()}.
             *
             * @return a number of minutes; equal to or greater than <code>0</code>.
             */
            @Configuration.Property(key = "from.minute")
            int fromMinute();

            /**
             * Specifies the additional hourly rate multiplied by 100.
             *
             * @return an integer; greater than <code>0</code>.
             */
            @Configuration.Property(key = "rate.by.100")
            int rateBy100();
        }

        /**
         * Represents an overtime rate level.
         */
        interface OvertimeLevel {

            /**
             * Specifies the number of hours after which this overtime rate applies.
             *
             * @return a number of minutes; greater than <code>0</code>.
             */
            @Configuration.Property(key = "threshold.minutes")
            int thresholdHours();

            /**
             * Specifies the minutes in addition to {@link #thresholdHours()}.
             *
             * @return a number of minutes; equal to or greater than <code>0</code>.
             */
            @Configuration.Property(key = "threshold.minutes")
            int thresholdMinutes();

            /**
             * Specifies the percent to multiply the hourly rate with.
             *
             * @return an integer; greater than <code>0</code>.
             */
            @Configuration.Property(key = "percent")
            int percent();
        }
    }
}
