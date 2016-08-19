package org.transmartproject.core.dataquery.highdim.vcf

enum GenomicVariantType {
    SNP, INS, DEL, DIV
    
    /**
     * Returns the Genomic Variant Type for a given reference and alternative
     *
     */
    static GenomicVariantType getGenomicVariantType(String ref, String alt) {
        def refCleaned = (ref ?: '').replaceAll(/[^ACGT]/, '')
        def altCleaned = (alt ?: '').replaceAll(/[^ACGT]/, '')

        if (refCleaned.length() == 1 && altCleaned.length() == 1)
            return SNP

        if (altCleaned.length() > refCleaned.length()
                && altCleaned.contains(refCleaned))
            return INS

        if (altCleaned.length() < refCleaned.length()
                && refCleaned.contains(altCleaned))
            return DEL

        DIV
    }
}
