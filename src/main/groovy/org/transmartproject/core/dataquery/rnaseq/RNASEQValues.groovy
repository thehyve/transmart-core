package org.transmartproject.core.dataquery.rnaseq
/**
 * Aggregates certain values, both measured and calculated,
 * for a certain gene in the context of a certain RNASeq trial.
 */
public interface RNASEQValues {

    /**
     * The raw read count
     *
     * For genes, this value is aggregated from fine grain read counts.
     *
     * @return the raw read count
     */
    Integer getReadCountValue()
}
