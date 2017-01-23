package org.transmartproject.core.querytool

/**
 * Enum to describe the class of high dimensional filter can be applied to this high dimensional data type.
 * This is used to display the correct UI in cohort selection based on high-dimensional data.
 */
enum HighDimensionFilterType {
    /**
     * Filter based on a single numeric value in the high dimensional dataset. Suitable for e.g. gene expression,
     * RNASeq etc.
     */
    SINGLE_NUMERIC,

    /**
     * Specific filter type for ACGH datasets
     */
     ACGH,

    /**
     * Specific filter type for VCF datasets
     */
     VCF,

    /**
     * Specific filter type for Two Region event datasets
     */
     TWO_REGION
}