package org.transmartproject.core.dataquery.highdim.vcf

/**
 * Exposes information about the genomic variants for a given cohort
 */
interface VcfCohortInfo {
    List<String> getAlleles()
    int getTotalAlleleCount()
    List<Integer> getAlleleCount()
    List<Double> getAlleleFrequency()
    List<GenomicVariantType> getGenomicVariantTypes()

    // Methods for MAF representation
    String getMajorAllele()
    String getMinorAllele()
    Double getMinorAlleleFrequency()
}
