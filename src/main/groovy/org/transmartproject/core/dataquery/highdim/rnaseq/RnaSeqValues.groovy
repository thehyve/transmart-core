package org.transmartproject.core.dataquery.highdim.rnaseq
/**
 * Aggregates certain values, both measured and calculated,
 * for a certain (assay, probe, gene or region) pair
 * in the context of a RNA sequencing high dimensional observation.
  */
public interface RnaSeqValues {

    Integer getReadcount()

    /**
     * The normalized read count value
     * Could e.g. be RPKM: 'reads per kilo base per million'
     *            or FPKM: 'fragments per kilo base per million'
     *
     * @return the normalized read count value
     */
     Double getNormalizedReadcount()

    /**
     * The 2 log of the normalized read count value
     * @return the 2 log of normalized read count value
     */
     Double getLogNormalizedReadcount()

    /**
     * The zscore based on the 2 log of the normalized read count values (for a specific gene or region) of all samples/assays in the data set
     * @return the zscore
     */
     Double getZscore()
}
