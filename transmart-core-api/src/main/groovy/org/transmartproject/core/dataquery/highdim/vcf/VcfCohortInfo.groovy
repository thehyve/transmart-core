package org.transmartproject.core.dataquery.highdim.vcf

/**
 * Exposes information about the genomic variants for a given cohort
 */
interface VcfCohortInfo {
    // Statistics
    List<String> getAlleles()
    int getTotalAlleleCount()
    List<Integer> getAlleleCount()
    List<Double> getAlleleFrequency()
    List<GenomicVariantType> getGenomicVariantTypes()

    // Lists of reference and alternatives
    String getReferenceAllele()
    List<String> getAlternativeAlleles()
    
    // Methods for MAF representation.
    String getMajorAllele()
    String getMinorAllele()
    Double getMinorAlleleFrequency()
}
