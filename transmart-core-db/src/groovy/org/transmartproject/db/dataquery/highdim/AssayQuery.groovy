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

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.db.dataquery.highdim.assayconstraints.AbstractAssayConstraint

/**
 * Collaborator for HighDimensionDataTypeResourceImpl.
 *
 * Takes care of issuing the query for assays.
 */
class AssayQuery {

    private static final int FETCH_SIZE = 5000

    List<AbstractAssayConstraint> constraints

    AssayQuery(List<AbstractAssayConstraint> constraints) {
        this.constraints = constraints
    }

    HibernateCriteriaBuilder prepareCriteriaWithConstraints() {
        HibernateCriteriaBuilder criteria = DeSubjectSampleMapping.createCriteria()

        /* we're calling a private method here... but I don't see a better way.
         * One option would be to use an hibernate Criteria from the start, but then
         * we'd have to express the constraints very awkwardly */
        criteria.createCriteriaInstance()

        constraints.each { c ->
            c.addConstraintsToCriteria criteria
        }

        criteria
    }

    /* Retrieves all the assays that satisfy the constraints passed to this
     * class constructor. Sorted by id asc */
    List<AssayColumn> retrieveAssays() {
        def criteria = prepareCriteriaWithConstraints()
        criteria.order 'id', 'asc'

        /* Again, we have to go deep into implementation details.
         * The problem is we cannot create the hibernate criteria
         * and execute the query in different statements; you're
         * supposed to do criteriaBuilder.list { .. constraints here .. }
         * Maybe we could rewrite some code so that everything happens inside
         * that closure, but for now let's break some abstractions.
         */
        try {
            criteria.instance.fetchSize = FETCH_SIZE
            criteria.instance.list().collect {
                new AssayColumnImpl(it)
            }
        } finally {
            // important, otherwise the connection leaks
            if (!criteria.participate) {
                criteria?.hibernateSession?.close()
            }
        }
    }

}
