package org.transmartproject.db.dataquery.highdim.vcf

import org.transmartproject.core.dataquery.highdim.vcf.VcfValues
import org.transmartproject.db.dataquery.highdim.vcf.VcfValuesImpl
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection
import grails.orm.HibernateCriteriaBuilder

/**
 * Created by j.hudecek on 21-2-14.
 */
class VcfValuesProjection implements CriteriaProjection<VcfValues> {

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder) {
        // VcfModule.prepareDataQuery already projects all the columns
    }

    @Override
    VcfValues doWithResult(Object object) {
        new VcfValuesImpl(object)
    }
}
