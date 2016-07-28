package org.fluidity.wages;

import java.time.ZonedDateTime;

/**
 * TODO
 */
public interface WageDetails {

    /**
     * TODO
     * @return
     */
    String personId();

    /**
     * TODO
     * @return
     */
    int salaryBy100();

    /**
     * TODO
     * @return
     */
    ZonedDateTime date();
}
