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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * A time interval from one date in some time zone to another date in some time zone. The time interval has a beginning and end.
 */
final class ZonedDateTimeInterval {

    final ZonedDateTime begin;
    final ZonedDateTime end;

    /**
     * Creates a new instance by specifying the beginning and the end of the time interval.
     *
     * @param begin the beginning of the time interval.
     * @param end   the end of the time interval.
     */
    ZonedDateTimeInterval(final ZonedDateTime begin, final ZonedDateTime end) {
        this.begin = begin;
        this.end = end;
    }

    /**
     * Computes the DST-aware length of the overlap between two intervals. On DST cutover when the time lines overlap, this method uses the later offset in all
     * cases.
     *
     * @param that the other interval.
     *
     * @return the length of the overlap between the two intervals; never <code>null</code>.
     */
    final Duration overlap(final ZonedDateTimeInterval that) {
        if (this.begin.isBefore(that.end) && that.begin.isBefore(this.end)) {
            final ZonedDateTime overlapBegin = (this.begin.isBefore(that.begin) ? that.begin : this.begin).withLaterOffsetAtOverlap();
            final ZonedDateTime overlapEnd = (this.end.isBefore(that.end) ? this.end : that.end).withLaterOffsetAtOverlap();

            return Duration.ofSeconds(overlapBegin.until(overlapEnd, ChronoUnit.SECONDS));
        } else {
            return Duration.ZERO;
        }
    }
}
