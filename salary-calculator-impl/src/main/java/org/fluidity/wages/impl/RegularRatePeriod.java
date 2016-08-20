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

/**
 * An interval during which some regular hourly rate applies.
 * <p>
 * This is an immutable value type.
 */
final class RegularRatePeriod {

    final int rateBy100;
    final LocalTimeInterval interval;

    /**
     * Creates a new instance.
     *
     * @param rateBy100 the hourly rate multiplied by 100 (the precision for dollar amounts)
     * @param begin     the beginning of the interval.
     * @param end       the end of the interval.
     */
    RegularRatePeriod(final int rateBy100, final LocalTime begin, final LocalTime end) {
        this.rateBy100 = rateBy100;
        this.interval = LocalTimeInterval.of(begin, end);
    }
}
