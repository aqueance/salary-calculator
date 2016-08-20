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

package org.fluidity.wages;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a single work shift.
 * <p>
 * This is an immutable value type.
 */
public final class ShiftDetails {

    public final String personId;
    public final String personName;
    public final LocalDate date;
    public final LocalTime begin;
    public final LocalTime end;

    public ShiftDetails(final String personId, final String personName, final LocalDate date, final LocalTime begin, final LocalTime end) {
        this.personId = personId;
        this.personName = personName;
        this.date = date;
        this.begin = begin;
        this.end = end;
    }
}
