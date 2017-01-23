package org.transmartproject.batch.highdim.datastd
/**
 * Interface for beans that hold chromosomal information
 */
interface ChromosomalRegionSupport {
    String getChromosome()

    Long getStartBp()

    Long getEndBp()
}
