package org.transmartproject.db.dataquery.highdim.vcf

import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.DEL
import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.DIV
import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.INS
import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.SNP

import java.util.List;
import java.util.Map;

import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType
import org.transmartproject.core.dataquery.highdim.vcf.VcfValues
import org.transmartproject.db.dataquery.highdim.AbstractDataRow
import org.transmartproject.db.dataquery.highdim.DefaultHighDimensionTabularResult;

class VcfCohortStatistics implements VcfValues {
    protected VcfDataRow dataRow
    
    // Cohort level properties
    Double maf = 0.0
    String mafAllele = "."
    
    List<String> alternativeAlleles = []
    List<GenomicVariantType> genomicVariantTypes = []

    VcfCohortStatistics( VcfDataRow dataRow ) {
        this.dataRow = dataRow
        
        computeCohortStatistics()
    }
    
    // Allele distribution for the current cohort
    @Lazy
    Map alleleDistribution = {
        def alleleDistribution = [:].withDefault { 0 }
        for (row in dataRow.data) {
            if ( !row )
                continue;
                
            def allele1 = row.allele1
            def allele2 = row.allele2
            alleleDistribution[allele1]++
            alleleDistribution[allele2]++
        }
        alleleDistribution
    }()

    @Override
    String getChromosome() {
        return dataRow.chromosome
    }
    
    @Override
    Long getPosition() {
        return dataRow.position
    }
    
    @Override
    String getRsId() {
        return dataRow.rsId
    }
    
    @Override
    String getReferenceAllele() {
        return dataRow.referenceAllele
    }

    
    @Override
    public Map<String, String> getAdditionalInfo() {
        return dataRow.additionalInfo
    }

    @Override
    public Double getQualityOfDepth() {
        // TODO Auto-generated method stub
        return dataRow.qualityOfDepth;
    }
        
    List<GenomicVariantType> getGenomicVariantTypes(Collection<String> altCollection) {
        getGenomicVariantTypes(referenceAllele, altCollection)
    }

    List<GenomicVariantType> getGenomicVariantTypes(String ref, Collection<String> altCollection) {
        altCollection.collect{ GenomicVariantType.getGenomicVariantType(ref, it) }
    }

    List<String> getAltAllelesByPositions(Collection<Integer> pos) {
        pos.collect { 
            println "" + it + " - " + dataRow.alternatives[it - 1] + " (" + dataRow.alternatives + ")"
            dataRow.alternatives[it - 1]
        }
    }
    
    /**
     * Computes cohort level statistics
     */
    protected computeCohortStatistics() {
        if( !alleleDistribution )
            return
            
        int total = alleleDistribution.values().sum()
        def altAlleleNums = alleleDistribution.keySet() - [DeVariantSubjectSummaryCoreDb.REF_ALLELE]
        
        additionalInfo['AN'] = total.toString()
        
        if (!altAlleleNums) {
            //R/R no mutation
            additionalInfo['AC'] = '.'
        } else {
            // Perform computations
            def altAlleleDistribution = alleleDistribution.subMap(altAlleleNums)
            def altAlleleFrequencies = altAlleleDistribution.collectEntries { [(it.key): it.value / (double) total] }
            def mafEntry = altAlleleFrequencies.max { it.value }
            
            // Store values
            def studyAlternatives = dataRow.alternatives.split( "," )
            additionalInfo['AC'] = altAlleleDistribution.values().join(',')
            alternativeAlleles = studyAlternatives[ altAlleleNums.collect { it - 1 } ]
            mafAllele = alternativeAlleles[altAlleleNums.asList().indexOf(mafEntry.key)]
            genomicVariantTypes = getGenomicVariantTypes(alternativeAlleles)
            maf = mafEntry.value
        }
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
}
