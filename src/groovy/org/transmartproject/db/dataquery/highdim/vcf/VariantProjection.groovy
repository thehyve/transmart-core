package org.transmartproject.db.dataquery.highdim.vcf

import grails.orm.HibernateCriteriaBuilder

import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

/**
 * Created by j.hudecek on 21-2-14.
 */
class VariantProjection implements CriteriaProjection<String> {

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder) {

    }

    @Override
    String doWithResult(Object object) {
        if (object == null) {
            return null /* missing data for an assay */
        }

        // Return the actual variant that the subject has
        return object.variant
    }
}
