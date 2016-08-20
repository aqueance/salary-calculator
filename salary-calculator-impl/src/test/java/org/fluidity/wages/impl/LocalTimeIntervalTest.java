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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LocalTimeIntervalTest {

    @Test
    public void testDurationNoOverlap() throws Exception {
        final ZoneId timeZone = ZoneId.systemDefault();
        final LocalDate date = LocalDate.now();

        final LocalTimeInterval _0_to_1 = LocalTimeInterval.of(LocalTime.of(0, 0), LocalTime.of(1, 0));
        final LocalTimeInterval _2_to_3 = LocalTimeInterval.of(LocalTime.of(2, 0), LocalTime.of(3, 0));

        final Duration overlap1 = _0_to_1.overlap(_2_to_3, date, timeZone);
        final Duration overlap2 = _2_to_3.overlap(_0_to_1, date, timeZone);

        Assert.assertNotNull(overlap1);
        Assert.assertTrue(overlap1.equals(overlap2));

        Assert.assertEquals(overlap1, Duration.ZERO);
    }

    @Test
    public void testOverlapDurationAtSameOffset() throws Exception {
        final ZoneId timeZone = ZoneId.systemDefault();
        final LocalDate date = LocalDate.now();

        final LocalTimeInterval _0_to_2 = LocalTimeInterval.of(LocalTime.of(0, 0), LocalTime.of(2, 0));
        final LocalTimeInterval _1_to_3 = LocalTimeInterval.of(LocalTime.of(1, 0), LocalTime.of(3, 0));

        final Duration overlap1 = _0_to_2.overlap(_1_to_3, date, timeZone);
        final Duration overlap2 = _1_to_3.overlap(_0_to_2, date, timeZone);

        Assert.assertNotNull(overlap1);
        Assert.assertTrue(overlap1.equals(overlap2));

        Assert.assertEquals(overlap1, Duration.of(1, ChronoUnit.HOURS));
    }

    @Test
    public void testEncapsulationAtSameOffset() throws Exception {
        final ZoneId timeZone = ZoneId.systemDefault();
        final LocalDate date = LocalDate.now();

        final LocalTimeInterval _0_to_3 = LocalTimeInterval.of(LocalTime.of(0, 0), LocalTime.of(3, 0));
        final LocalTimeInterval _1_to_2 = LocalTimeInterval.of(LocalTime.of(1, 0), LocalTime.of(2, 0));

        final Duration overlap1 = _0_to_3.overlap(_1_to_2, date, timeZone);
        final Duration overlap2 = _1_to_2.overlap(_0_to_3, date, timeZone);

        Assert.assertNotNull(overlap1);
        Assert.assertTrue(overlap1.equals(overlap2));

        Assert.assertEquals(overlap1, Duration.of(1, ChronoUnit.HOURS));
    }

    @Test
    public void testOverlapDurationAtOffsetGap() throws Exception {
        final ZoneId timeZone = ZoneId.of("Europe/Helsinki");
        final LocalDate date = LocalDate.of(2016, Month.MARCH, 27);

        final LocalTimeInterval _1_to_4 = LocalTimeInterval.of(LocalTime.of(1, 0), LocalTime.of(4, 0));
        final LocalTimeInterval _2_to_5 = LocalTimeInterval.of(LocalTime.of(2, 0), LocalTime.of(5, 0));

        final Duration overlap1 = _1_to_4.overlap(_2_to_5, date, timeZone);
        final Duration overlap2 = _2_to_5.overlap(_1_to_4, date, timeZone);

        Assert.assertNotNull(overlap1);
        Assert.assertTrue(overlap1.equals(overlap2));

        Assert.assertEquals(overlap1, Duration.of(1, ChronoUnit.HOURS));
    }

    @Test
    public void testOverlapDurationAtOffsetOverlap() throws Exception {
        final ZoneId timeZone = ZoneId.of("Europe/Helsinki");
        final LocalDate date = LocalDate.of(2016, Month.OCTOBER, 30);

        final LocalTimeInterval _1_to_3 = LocalTimeInterval.of(LocalTime.of(1, 0), LocalTime.of(3, 0));
        final LocalTimeInterval _2_to_5 = LocalTimeInterval.of(LocalTime.of(2, 0), LocalTime.of(5, 0));

        final Duration overlap1 = _1_to_3.overlap(_2_to_5, date, timeZone);
        final Duration overlap2 = _2_to_5.overlap(_1_to_3, date, timeZone);

        Assert.assertNotNull(overlap1);
        Assert.assertTrue(overlap1.equals(overlap2));

        Assert.assertEquals(overlap1, Duration.of(2, ChronoUnit.HOURS));
    }
}
