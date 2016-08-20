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

/**
 * Represents a monthly salary for a person.
 * <p>
 * This is an immutable value type.
 */
@SuppressWarnings("WeakerAccess")
public final class SalaryDetails {

    public final String personId;
    public final String personName;
    public final LocalDate month;
    public final int amountBy100;

    public SalaryDetails(final String personId, final String personName, final LocalDate month, final int amountBy100) {
        this.personId = personId;
        this.personName = personName;
        this.month = month;
        this.amountBy100 = amountBy100;
    }

    /**
     * Returns the formatted dollar amount.
     *
     * @return a text; never <code>null</code>.
     */
    public String amount() {
        return String.format("$%d.%d", amountBy100 / 100, amountBy100 % 100);
    }

    @Override
    public String toString() {
        return String.format("%s, %s, %d/%d, %s", personId, personName, month.getMonthValue(), month.getYear(), amount());
    }
}
