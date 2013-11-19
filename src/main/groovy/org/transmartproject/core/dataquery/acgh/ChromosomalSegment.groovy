package org.transmartproject.core.dataquery.acgh

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Specifies segment on chromosome.
 * It's supposed to be wider than region and be used to query acgh data
 *
 * @deprecated to be moved to new package
 */
@EqualsAndHashCode
@ToString
@Deprecated
public class ChromosomalSegment {

    /**
     * An identifier for the chromosome
     * @return
     */
    String chromosome

    /**
     * The base pair number that identifies the starting position for this
     * interval in its chromosome.
     *
     * @return the start of the interval in the chromosome
     */
    Long start

    /**
     * The base pair number that identifies the final position for this
     * interval in its chromosome.
     * @return the end of the interval in the chromosome
     */
    Long end

}
