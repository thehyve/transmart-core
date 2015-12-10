/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.dataquery.highdim.projections

import com.google.common.collect.ImmutableMap
import grails.orm.HibernateCriteriaBuilder
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.Projections
import org.transmartproject.core.dataquery.highdim.projections.AllDataProjection

/**
 * This projection collects all the fields specified in the constructor as a map from field name to value
 */
class AllDataProjectionImpl implements CriteriaProjection<Map<String, Object>>, AllDataProjection {

    static Log LOG = LogFactory.getLog(this)

    // These should actually be some kind of OrderedMap that guarantees a specific iteration order, but there is no
    // such interface in general use (only some project specific ones, e.g. org.apache.commons.collections.OrderedMap).
    // As the next best thing we specify a concrete implementation that has stable ordering.
    ImmutableMap<String, Class> dataProperties
    ImmutableMap<String, Class> rowProperties

    AllDataProjectionImpl(ImmutableMap<String, Class> dataProperties, ImmutableMap<String, Class> rowProperties) {
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

        for (String field : dataProperties.keySet()) {
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
