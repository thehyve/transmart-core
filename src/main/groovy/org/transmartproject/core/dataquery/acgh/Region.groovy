package org.transmartproject.core.dataquery.acgh

import org.transmartproject.core.dataquery.Platform
import org.transmartproject.core.dataquery.PlatformMarkerType

/**
 * A region is a set of contiguous probes (with respect to the chromosomal
 * position targeted) that were grouped together by the aCGH algorithms based
 * on similar calls across a certain set of samples for such probes.
 *
 * The set of continuous implies a contiguous chromosomal base pair interval,
 * which is also part of the identity of the region (indeed,
 * at this point we may no longer have information about which probes
 * contributed to form the region).
 */
public interface Region {

    /**
     * The marker type for this platform; see {@link Platform#getMarkerType()}.
     */
    public final static PlatformMarkerType MARKER_TYPE = PlatformMarkerType.CHROMOSOMAL_REGION

    /**
     * A unique numeric identifier for the region.
     *
     * @return unique numeric identifier
     */
    Long getId()

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
     * The number of probes that were grouped in order to form this region.
     *
     * @return number of probes aggregated in this region
     */
    Integer getNumberOfProbes()

}
