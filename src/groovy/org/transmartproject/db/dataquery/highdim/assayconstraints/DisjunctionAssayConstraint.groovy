package org.transmartproject.db.dataquery.highdim.assayconstraints

import grails.orm.HibernateCriteriaBuilder

class DisjunctionAssayConstraint extends AbstractAssayConstraint {

    List<AbstractAssayConstraint> constraints

    @Override
    void addConstraintsToCriteria(HibernateCriteriaBuilder criteria) {
        criteria.with {
            or {
                constraints.each { AbstractAssayConstraint subConstraint ->
                    subConstraint.addConstraintsToCriteria criteria
                }
            }
        }
    }
}
