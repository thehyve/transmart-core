package org.transmartproject.db.dataquery.highdim.projections

import grails.orm.HibernateCriteriaBuilder
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections
import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection

/**
 * Created by jan on 12/17/13.
 */

/**
 * This projection collects all the fields specified in the constructor as a map from field name to value
 */
class AllDataProjectionImpl implements CriteriaProjection<Map<String, Object>>, AllDataProjection {

    static Log LOG = LogFactory.getLog(this)

    Collection<String> dataProperties
    Collection<String> rowProperties

    AllDataProjectionImpl(Collection<String> dataProperties, Collection<String> rowProperties) {
        this.dataProperties = dataProperties
        this.rowProperties = rowProperties
    }

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder){
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

        for (String field : dataProperties) {
            // add an alias to make this ALIAS_TO_ENTITY_MAP-friendly
            projection.add(
                    Projections.alias(
                            Projections.property(field),
                            field))
        }
    }


    @Override
    Map<String, Object> doWithResult(Object obj) {
        if (obj == null)
            return null /* missing data for an assay */

        def map = obj.clone() as Map<String, Object>
        // assay is a hibernate association, that is not supported in the stateless session we are using.
        // It is already provided by the data row
        map.remove('assay')
        map
    }

}
