package org.transmartproject.core.dataquery.highdim.vcf

interface VcfValues {
    String getChromosome()
    Long getPosition()
    String getRsId()
    String getMafAllele()
    Double getMaf()
    Double getQualityOfDepth()
    String getReferenceAllele()
    List<String> getAlternativeAlleles()
    Map<String, String> getAdditionalInfo()
    List<GenomicVariantType> getGenomicVariantTypes()
}
