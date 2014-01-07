package org.transmartproject.db.dataquery.highdim.projections

import grails.orm.HibernateCriteriaBuilder
import groovy.transform.Immutable
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections

@Immutable
class SimpleRealProjection implements CriteriaProjection<Double> {

    static Log LOG = LogFactory.getLog(this)

    String property

    private Boolean addedProjection = false

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

        if (addedProjection) {
            if (obj.getClass().isArray()) {
                /* projection with default ResultTransformer results in
                 * an Object[]. Take the last element */
                return obj[obj.length - 1]
            } else if (obj instanceof Map) {
                // Using the ALIAS_TO_ENTITY_MAP transformer we get a nifty map
                return obj[property]
            }

        } else {
            return obj."$property"
        }
    }
}
