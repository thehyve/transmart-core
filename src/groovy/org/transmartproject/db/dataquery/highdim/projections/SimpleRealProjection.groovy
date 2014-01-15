package org.transmartproject.db.dataquery.highdim.projections

import grails.orm.HibernateCriteriaBuilder
import groovy.transform.Immutable
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections

class SimpleRealProjection implements CriteriaProjection<Double> {

    static Log LOG = LogFactory.getLog(this)

    String property

    private boolean addedProjection = false

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder) {
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
                        Projections.property(this.property),
                        this.property))

        addedProjection = true
    }

    @Override
    Double doWithResult(Object obj) {
        if (obj == null) {
            return null /* missing data for an assay */
        }

        if (addedProjection && obj.getClass().isArray()) {
            /* projection with default ResultTransformer results in
             * an Object[]. Take the last element */
            return obj[-1]
        }
        // If the ALIAS_TO_ENTITY_MAP transformer was used, obj is a map, else we just take the corresponding property.
        return obj."$property"
    }
}
