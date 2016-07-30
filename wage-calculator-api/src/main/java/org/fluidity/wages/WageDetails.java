package org.fluidity.wages;

import java.time.LocalDate;

/**
 * Represents a monthly wage for a person.
 */
public final class WageDetails {

    public final String personId;
    public final String personName;
    public final LocalDate date;
    public final int amountBy100;

    public WageDetails(final String personId, final String personName, final LocalDate date, final int amountBy100) {
        this.personId = personId;
        this.personName = personName;
        this.date = date;
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
        return String.format("%s, %s, %d/%d, %s", personId, personName, date.getMonthValue(), date.getYear(), amount());
    }
}
