package org.transmartproject.core.dataquery.highdim.chromoregion

import org.transmartproject.core.dataquery.highdim.Platform

/**
 * A region is a platform- (and therefore data type-) bound portion of a
 * chromosome.
 *
 * For aCGH, is the result of the aggregation of several probes, but, in
 * general, it's another platform element, that, when combined with an
 * assay, identifies a data point in high dimensional data.
 */
public interface Region {

    /**
     * A unique numeric identifier for the region.
     *
     * @return unique numeric identifier
     */
    Long getId()

    /**
     * A name that identities the region
     *
     * @return a friendly region name for the region or null
     */
    String getName()

    /**
     * A cytoband name that identities the region
     *
     * @return a friendly cytoband name for the region or null
     */
    String getCytoband()

    /**
     * The synthetic platform that groups all the regions for the same trial.
     * Technically, not an actual platform.
     *
     * @return the bogus platform that groups regions for the same trial
     */
    Platform getPlatform()

    /**
     * An identifier for the chromosome
     * @return
     */
    String getChromosome()

    /**
     * The base pair number that identifies the starting position for this
     * region in its chromosome.
     *
     * @return the start of the region in the region's chromosome
     */
    Long getStart()

    /**
     * The base pair number that identifies the final position for this
     * region in its chromosome.
     * @return
     */
    Long getEnd()

    /**
     * The number of probes that were grouped in order to form this region,
     * if applicable.
     *
     * @return number of probes aggregated in this region or null if inapplicable
     */
    Integer getNumberOfProbes()

}
