package org.transmartproject.db.dataquery.highdim.vcf

import grails.orm.HibernateCriteriaBuilder

import org.transmartproject.core.dataquery.highdim.vcf.VcfValues
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

/**
 * Created by j.hudecek on 21-2-14.
 */
class CohortProjection implements CriteriaProjection<VcfValues> {

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder) {

    }

    @Override
    VcfValues doWithResult(Object object) {
        new VcfValuesImpl(object)
    }
}
