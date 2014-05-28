package org.transmartproject.db.dataquery.highdim.vcf

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

/**
 * Created by j.hudecek on 21-2-14.
 */
class VariantProjection implements CriteriaProjection<String> {

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder) {
        // Retrieving the criteriabuilder projection (which contains
        // the fields to be retrieved from the database)
        // N.B. This is a different object than the object we are 
        // currently in, although they are both called Projection!
        def projection = builder.instance.projection

        if (!(projection instanceof ProjectionList)) {
            throw new IllegalArgumentException("doWithCriteriaBuilder method" +
                    " requires a Hibernate Projectionlist to be set.")
        }

        // add an alias to make this ALIAS_TO_ENTITY_MAP-friendly
        projection.add(
                Projections.alias(
                        Projections.property( "summary.variant"),
                        "variant"))
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
