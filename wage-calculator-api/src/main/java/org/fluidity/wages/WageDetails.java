package org.fluidity.wages;

import java.time.LocalDate;

/**
 * TODO
 */
public final class WageDetails {

    public final String personId;
    public final LocalDate date;
    public final int amountBy100;

    public WageDetails(final String personId, final LocalDate date, final int amountBy100) {
        this.personId = personId;
        this.date = date;
        this.amountBy100 = amountBy100;
    }
}
