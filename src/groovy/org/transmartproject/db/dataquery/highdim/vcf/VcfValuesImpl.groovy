package org.transmartproject.db.dataquery.highdim.vcf

import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.vcf.GenomicVariantType
import org.transmartproject.core.dataquery.highdim.vcf.VcfValues
import org.transmartproject.db.dataquery.highdim.AbstractDataRow

class VcfValuesImpl extends AbstractDataRow implements VcfValues, RegionRow {
    String chromosome
    Long position
    String rsId
    String mafAllele
    Double maf
    Double qualityOfDepth
    String referenceAllele
    List<String> alternativeAlleles
    Map<String, String> additionalInfo
    List<GenomicVariantType> genomicVariantTypes

    List<VcfValues> details

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
