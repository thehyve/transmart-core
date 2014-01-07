package org.transmartproject.db.dataquery.highdim.dataconstraints

import grails.orm.HibernateCriteriaBuilder

class DisjunctionDataConstraint implements CriteriaDataConstraint {

    List<CriteriaDataConstraint> constraints

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria) {
        criteria.with {
            or {
                constraints.each { CriteriaDataConstraint subConstraint ->
                    subConstraint.doWithCriteriaBuilder criteria
                }
            }
        }
    }
}
