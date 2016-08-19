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

package org.transmartproject.db.dataquery.highdim

import grails.gorm.DetachedCriteria
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.db.dataquery.AbstractEntityQuery
import org.transmartproject.db.dataquery.highdim.assayconstraints.AssayCriteriaConstraint

/**
 * Collaborator for HighDimensionDataTypeResourceImpl.
 *
 * Takes care of issuing the query for assays.
 */
class AssayQuery extends AbstractEntityQuery<Assay> {

    private final DetachedCriteria<DeSubjectSampleMapping> criteria

    AssayQuery(List<AssayCriteriaConstraint> constraints) {
        this.criteria =
            DeSubjectSampleMapping.where {
                constraints.each { AssayCriteriaConstraint assayConstraint ->
                    assayConstraint.addToCriteria(it)
                }
                order 'id'
            }
    }

    @Override
    DetachedCriteria<Assay> forEntities() {
        criteria
    }

}
