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

package org.transmartproject.db.dataquery.highdim.assayconstraints

import groovy.transform.Canonical
import org.grails.datastore.mapping.query.api.Criteria

@Canonical
class MarkerTypeCriteriaConstraint implements AssayCriteriaConstraint {

    List platformNames

    @Override
    void addToCriteria(Criteria criteria) {
        /** @see org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping */
        // This criteria does not work with the InQuery.addIn workaround
        // because of the 'platform' addition. It is very unlikely to have more than 1000 platform names,
        // so for now it will not cause problems.
        criteria.platform {
            'in' 'markerType', platformNames
        }
    }
}
