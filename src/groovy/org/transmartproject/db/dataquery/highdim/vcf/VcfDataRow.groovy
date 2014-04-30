package org.transmartproject.db.dataquery.highdim.vcf

import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.DEL
import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.DIV
import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.INS
import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.SNP

import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType
import org.transmartproject.core.dataquery.highdim.vcf.VcfValues
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

class VcfDataRow extends AbstractDataRow implements VcfValues, RegionRow {

    // Chromosome to define the position
    String chromosome
    Long position
    String rsId
    
    // Reference and alternatives for this position
    String referenceAllele
    String alternatives
    
    // Study level properties
    String quality
    String filter
    String info
    String format
    String variants
    
    @Lazy
    Double qualityOfDepth = {
        if (getAdditionalInfo()['QD'] && additionalInfo['QD'].isNumber()) {
            Double.valueOf(additionalInfo['QD'])
        } else {
            quality as double;
        }
    }()

    @Lazy
    Map<String, String> additionalInfo = {
        parseVcfInfo( info )
    }()
    
    @Lazy
    VcfCohortStatistics cohortStatistics = {
       new VcfCohortStatistics(this) 
    }()

    @Override
    String getMafAllele() {
        cohortStatistics.mafAllele
    }
    
    @Override
    Double getMaf() {
        cohortStatistics.maf
    }
    
    @Override 
    List<GenomicVariantType> getGenomicVariantTypes() {
        cohortStatistics.genomicVariantTypes
    }
    
    @Override
    List<String> getAlternativeAlleles() {
        cohortStatistics.alternativeAlleles
    }
    
    //RegionRow implementation
    @Override
    String getLabel() {
        return 'VCF: ' + chromosome + ":" + position
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
    
    private Map parseVcfInfo(String info) {
        if (!info) {
            return [:]
        }

        info.split(';').collectEntries {
            def keyValues = it.split('=')
            [(keyValues[0]): keyValues.length > 1 ? keyValues[1] : 'Yes']
        }
    }

}
