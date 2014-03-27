package org.transmartproject.db.dataquery.highdim.vcf

import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType
import org.transmartproject.core.dataquery.highdim.vcf.VcfValues
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.DEL
import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.DIV
import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.INS
import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.SNP

class VcfValuesImpl extends AbstractDataRow implements VcfValues, RegionRow {

    String mafAllele
    Double maf
    Double qualityOfDepth

    List<String> alternativeAlleles

    Map<String, String> additionalInfo
    List<GenomicVariantType> genomicVariantTypes

    Map summary

    VcfValuesImpl(List collectedEntries) {
        data = collectedEntries
        summary = collectedEntries.find()[0]
        additionalInfo = parseVcfInfo(summary.info)

        Map alleleDistribution = getAlleleDistribution(collectedEntries)

        if (!alleleDistribution)
            throw new InvalidArgumentsException("alleleDistribution can't be null")

        int total = alleleDistribution.values().sum()
        def altAlleleNums = alleleDistribution.keySet() - [DeVariantSubjectSummaryCoreDb.REF_ALLELE]

        if (!altAlleleNums) {
            //R/R no mutation
            additionalInfo['AC'] = '.'
            alternativeAlleles = []
            mafAllele = '.'
            genomicVariantTypes = []
            maf = 0
        } else {
            def altAlleleDistribution = alleleDistribution.subMap(altAlleleNums)
            def altAlleleFrequencies = altAlleleDistribution.collectEntries { [(it.key): it.value / (double) total] }
            def mafEntry = altAlleleFrequencies.max { it.value }

            additionalInfo['AC'] = altAlleleDistribution.values().join(',')
            alternativeAlleles = getAltAllelesByPositions(altAlleleNums)
            mafAllele = alternativeAlleles[altAlleleNums.asList().indexOf(mafEntry.key)]
            genomicVariantTypes = getGenomicVariantTypes(alternativeAlleles)
            maf = mafEntry.value
        }
        additionalInfo['AN'] = total.toString()



    }


    private static Map getAlleleDistribution(List collectedEntries) {
        def alleleDistribution = [:].withDefault { 0 }
        for (row in collectedEntries) {
            if (row == null)
                continue;
            def allele1 = row[0].allele1
            def allele2 = row[0].allele2
            alleleDistribution[allele1]++
            alleleDistribution[allele2]++
        }
        alleleDistribution
    }

    String getChromosome() {
        return  summary.chr
    }
    Long getPosition() {
        return summary.pos
    }
    String getRsId() {
        return summary.rsId
    }
    String getReferenceAllele() {
        return summary.ref
    }

    @Override
    Double getQualityOfDepth() {
        if (!getAdditionalInfo()['QD'] || !additionalInfo['QD'].isNumber()) {
            return summary.quality as double;
        }
        Double.valueOf(additionalInfo['QD'])
    }

    @Override
    List<String> getAlternativeAlleles() {
        if(alternativeAlleles == null) {
            alternativeAlleles = parseCsvString(summary.alt)
        }
        alternativeAlleles
    }


    List<GenomicVariantType> getGenomicVariantTypes(Collection<String> altCollection) {
        getGenomicVariantTypes(summary.ref, altCollection)
    }

    List<GenomicVariantType> getGenomicVariantTypes(String ref, Collection<String> altCollection) {
        altCollection.collect{ getGenomicVariantType(summary.ref, it) }
    }

    static GenomicVariantType getGenomicVariantType(String ref, String alt) {
        def refCleaned = (ref ?: '').replaceAll(/[^ACGT]/, '')
        def altCleaned = (alt ?: '').replaceAll(/[^ACGT]/, '')

        if(refCleaned.length() == 1 && altCleaned.length() == 1)
            return SNP

        if(altCleaned.length() > refCleaned.length()
                && altCleaned.contains(refCleaned))
            return INS

        if(altCleaned.length() < refCleaned.length()
                && refCleaned.contains(altCleaned))
            return DEL

        DIV
    }

    List<String> getAltAllelesByPositions(Collection<Integer> pos) {
        pos.collect { getAlternativeAlleles()[it - 1] }
    }

    private Map parseVcfInfo(String info) {
        if (!info) {
            return [:]
        }

        info.split(';').collectEntries {
            def keyValues = it.split('=')
            [(keyValues[0]): keyValues.length > 1 ? keyValues[1] : 'Yes']
        }
    }

    private List<Double> parseNumbersList(String numbersString) {
        parseCsvString(numbersString) {
            it.isNumber() ? Double.valueOf(it) : null
        }
    }

    private List parseCsvString(String string, Closure typeConverterClosure = { it }) {
        if (!string) {
            return []
        }

        string.split(/\s*,\s*/).collect {
            typeConverterClosure(it)
        }
    }

    //RegionRow implementation
    @Override
    String getLabel() {
        return 'vcf'
    }

    @Override
    Long getId() {
        return rsId
    }

    @Override
    String getName() {
        return rsId
    }

    @Override
    String getCytoband() {
        return rsId
    }

    @Override
    Platform getPlatform() {
        return null
    }

    @Override
    Long getStart() {
        return position
    }

    @Override
    Long getEnd() {
        return position
    }

    @Override
    Integer getNumberOfProbes() {
        return 1
    }
}
