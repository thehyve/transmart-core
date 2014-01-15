package org.transmartproject.db.dataquery.highdim.projections

import grails.orm.HibernateCriteriaBuilder
import groovy.transform.Immutable
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections

/**
 * Created by jan on 12/17/13.
 */

/**
 * This projection collects all the fields specified in the constructor as a map from field name to value
 */
class GenericProjection implements CriteriaProjection<Map<String, Object>>{

    static Log LOG = LogFactory.getLog(this)

    Collection<String> fields

    GenericProjection(Collection<String> fields) {
        this.fields = fields
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

        for (String field : fields) {
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
