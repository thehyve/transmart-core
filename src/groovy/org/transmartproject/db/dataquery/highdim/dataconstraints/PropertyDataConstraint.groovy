package org.transmartproject.db.dataquery.highdim.dataconstraints

import grails.orm.HibernateCriteriaBuilder

class PropertyDataConstraint implements CriteriaDataConstraint {

    String property

    Object values // list or single object

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria) {
        criteria.with {
            if (values instanceof Collection) {
                'in' property, values
            } else {
                eq property, values
            }
        }
    }
}
