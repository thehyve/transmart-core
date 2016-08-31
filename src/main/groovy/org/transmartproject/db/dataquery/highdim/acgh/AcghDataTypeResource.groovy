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

package org.transmartproject.db.dataquery.highdim.acgh

import groovy.transform.InheritConstructors
import org.hibernate.Query
import org.transmartproject.core.dataquery.highdim.acgh.ChromosomalSegment
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.exceptions.EmptySetException
import org.transmartproject.db.dataquery.highdim.AssayQuery
import org.transmartproject.db.dataquery.highdim.HighDimensionDataTypeResourceImpl

/**
 * Created by glopes on 11/22/13.
 */
@InheritConstructors
class AcghDataTypeResource extends HighDimensionDataTypeResourceImpl {

    List<ChromosomalSegment> retrieveChromosomalSegments(
            List<AssayConstraint> assayConstraints) {

        def assayQuery = new AssayQuery(assayConstraints)
        def assayPlatformsQuery = assayQuery.forEntities().where {
            projections {
                distinct 'platform.id'
                id()
            }
        }

        def platformIds = assayPlatformsQuery.list().collect { it[0] } as Set

        if (platformIds.empty) {
            throw new EmptySetException(
                    'No assays satisfy the provided criteria')
        }

        log.debug "Now getting regions for platforms: $platformIds"

        Query q = module.sessionFactory.currentSession.createQuery('''
            SELECT region.chromosome, min(region.start), max(region.end)
            FROM DeChromosomalRegion region
            WHERE region.platform.id in (:platformIds)
            GROUP BY region.chromosome''')
        q.setParameterList('platformIds', platformIds)

        q.list().collect {
            new ChromosomalSegment(chromosome: it[0], start: it[1], end: it[2])
        } ?: {
            throw new EmptySetException(
                    "No regions found for platform ids: $platformIds")
        }()
    }
}
