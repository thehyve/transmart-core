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

    List<AbstractAssayConstraint> constraints

    AssayQuery(List<AbstractAssayConstraint> constraints) {
        this.constraints = constraints
    }

    /* Retrives all the assays that satisfy the constraints passed to this
     * class constructor. Sorted by id asc */
    List<AssayColumn> retrieveAssays() {
        HibernateCriteriaBuilder criteria = DeSubjectSampleMapping.createCriteria()

        /* we're calling a private method here... but I don't see a better way.
         * One option would be to use an hibernate Criteria from the start, but then
         * we'd have to express the constraints very awkwardly */
        criteria.createCriteriaInstance()

        criteria.order 'id', 'asc'

        constraints.each { c ->
            c.addConstraintsToCriteria criteria
        }

        criteria.instance.list().collect {
            new AssayColumnImpl(it)
        }
    }

}
