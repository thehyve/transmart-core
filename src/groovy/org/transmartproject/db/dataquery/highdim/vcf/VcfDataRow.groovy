package org.transmartproject.db.dataquery.highdim.vcf

import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.DEL
import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.DIV
import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.INS
import static org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType.SNP

import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.vcf.VcfCohortInfo
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
    
    List<String> getAlternativeAlleles() {
        return alternatives.split(",")
    }
    
    @Lazy
    Double qualityOfDepth = {
        if (infoFields['QD'] && infoFields['QD'].isNumber()) {
            Double.valueOf(infoFields['QD'])
        } else {
            quality as double;
        }
    }()

    @Lazy
    Map<String, String> infoFields = {
        parseVcfInfo( info )
    }()
    
    @Lazy
    List<String> formatFields = {
        format.split( ":" )
    }()

    @Lazy
    VcfCohortInfo cohortInfo = {
       new VcfCohortStatistics(this) 
    }()

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
