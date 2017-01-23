package org.transmartproject.core.dataquery.highdim.tworegion

/**
 * Represents metadata about one junction as part of an event of a two region variant
 */
interface JunctionEvent {
    /**
     * Reference to the event for which these values were measured
     */
    Event getEvent()

    /**
     * number of reads in the whole span
     */
    Integer getReadsSpan()

    /**
     * number of reads spanning the junction
     */
    Integer getReadsJunction()

    /**
     * number of spanning mate pairs
     */
    Integer getPairsSpan()

    /**
     * number of spanning mate pairs where one end spans a fusion
     */
    Integer getPairsJunction()

    /**
     * number of mate pairs that support the fusion and whose one end spans the fusion
     */
    Integer getPairsEnd()

    /**
     * number of reads that contradict the fusion by mapping to only one of the chromosomes
     */
    Integer getPairsCounter()

    /**
     * frequency in the baseline set of genomes for the junction
     */
    Double getBaseFreq()
}
