package org.transmartproject.core.dataquery.highdim.acgh
/**
 * Aggregates certain values, both measured and calculated,
 * for a certain (assay, probe or region) pair in the context of a certain
 * array comparative genomic hybridization trial.
 */
public interface AcghValues {

    /**
     * The log2 ratio value for the (assay, probe/region) pair represented in
     * this value, for the trial at issue.
     *
     * For region stage values, this value is a central tendency of the
     * probes aggregated by the region.
     *
     * @return the log2 ratio value
     */
    Double getChipCopyNumberValue()

    /**
     * The log2 ratio value for the (assay, segment) pair represented in
     * this value, for the trial at issue.
     *
     * For a certain patient, this value will be identical in a segment for
     * the set of contiguous probes that for the segment.
     *
     * For region stage values, this value is the result of a central tendency
     * statistic.
     *
     * @return the log2 ratio value
     */
    Double getSegmentCopyNumberValue()

    /**
     * A hard call on which type of copy number variation the underlying
     * segment or region has experienced
     *
     * @return the copy number hard call
     */
    CopyNumberState getCopyNumberState()

    /**
     * The probability ("soft call") of this region or segment having
     * suffered a copy number loss.
     *
     * @return probability of loss event
     */
    Double getProbabilityOfLoss()

    /**
     * The probability ("soft call") of this region or segment not
     * having suffered any kind of copy number alteration.
     *
     * @return probability of normal event
     */
    Double getProbabilityOfNormal()

    /**
     * The probability ("soft call") that this region or segment has
     * suffered a single copy-number gain.
     *
     * @return probability of gain event
     */
    Double getProbabilityOfGain()

    /**
     * The probability ("soft call") of that this region or segment having
     * suffered multiple copy-number gains.
     *
     * @return probability of amplification event
     */
    Double getProbabilityOfAmplification()
}
