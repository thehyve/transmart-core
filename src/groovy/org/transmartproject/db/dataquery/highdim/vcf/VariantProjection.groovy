package org.transmartproject.db.dataquery.highdim.vcf

import grails.orm.HibernateCriteriaBuilder

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections
import org.transmartproject.db.dataquery.highdim.projections.CriteriaProjection

/**
 * Created by j.hudecek on 21-2-14.
 */
class VariantProjection implements CriteriaProjection<String> {
    static Log LOG = LogFactory.getLog(this)
    
    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder) {
        // Retrieving the criteriabuilder projection (which contains
        // the fields to be retrieved from the database)
        // N.B. This is a different object than the object we are 
        // currently in, although they are both called Projection!
        def projection = builder.instance.projection
        
        if (!projection) {
            LOG.debug 'Skipping criteria manipulation because projection is not set'
            return
        }
        if (!(projection instanceof ProjectionList)) {
            LOG.debug 'Skipping criteria manipulation because projection ' +
                    'is not a ProjectionList'
            return
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
