package org.transmartproject.db.dataquery.highdim.dataconstraints

import grails.orm.HibernateCriteriaBuilder

class NoopDataConstraint implements CriteriaDataConstraint {

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria) {
        // purposefully left empty
    }
}
