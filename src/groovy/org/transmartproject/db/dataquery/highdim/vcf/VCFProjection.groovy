package org.transmartproject.db.dataquery.highdim.vcf

import org.transmartproject.core.dataquery.highdim.vcf.VcfValues
import org.transmartproject.db.dataquery.highdim.vcf.VcfValuesImpl
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection
import grails.orm.HibernateCriteriaBuilder

/**
 * Created by j.hudecek on 21-2-14.
 */
class VCFProjection implements CriteriaProjection<VcfValues> {

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder) {

    }

    @Override
    VcfValues doWithResult(Object list) {
        // Convert the row of data into a proper datarow
//        def firstNonNullCell = list.find()
//        new VcfDataRow(
//                probe:         firstNonNullCell[0].probeName,
//                geneSymbol:    firstNonNullCell[0].geneSymbol,
//                geneId:        firstNonNullCell[0].geneId,
//                assayIndexMap: assayIndexMap,
//                data:          list.collect { projection.doWithResult it?.getAt(0) }
//        )
        
        new VcfValuesImpl(object)
    }
}
