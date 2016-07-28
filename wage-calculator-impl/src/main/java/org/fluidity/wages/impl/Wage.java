package org.fluidity.wages.impl;

import java.time.ZonedDateTime;

import org.fluidity.wages.WageDetails;

/**
 * TODO: javadoc...
 */
final class Wage implements WageDetails {

    private final String personId;
    private final ZonedDateTime date;
    private final int salaryBy100;

    public Wage(final String personId, final ZonedDateTime date, final int salaryBy100) {
        this.personId = personId;
        this.date = date;
        this.salaryBy100 = salaryBy100;
    }

    @Override
    public String personId() {
        return personId;
    }

    @Override
    public int salaryBy100() {
        return salaryBy100;
    }

    @Override
    public ZonedDateTime date() {
        return date;
    }
}
