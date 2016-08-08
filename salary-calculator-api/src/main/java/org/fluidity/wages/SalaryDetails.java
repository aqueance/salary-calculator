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
