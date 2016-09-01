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

import grails.orm.HibernateCriteriaBuilder
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
