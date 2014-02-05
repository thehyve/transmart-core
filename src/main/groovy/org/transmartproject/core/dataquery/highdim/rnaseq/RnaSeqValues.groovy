package org.transmartproject.core.dataquery.highdim.rnaseq
/**
 * Aggregates certain values, both measured and calculated,
 * for a certain (assay, probe, gene or region) pair
 * in the context of a RNA sequencing high dimensional observation.
  */
public interface RnaSeqValues {

    Integer getReadCount()

    /**
     * The 'reads per kilo base per million' value
     *
     * @return the RPKM value
     */
     //Double getRpkmValue()

    /**
     * The 'fragments per kilo base per million' value
     *
     * @return the FPKM value
     */
    //Double getFpkmValue()
}
