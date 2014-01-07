package org.transmartproject.db.dataquery.highdim.dataconstraints

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.criterion.Restrictions

class PropertyDataConstraint implements CriteriaDataConstraint {

    String property

    Object values // list or single object

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria) {
        criteria.with {
            if (values instanceof Collection) {
                if (!values.isEmpty()) {
                    'in' property, values
                } else {
                    criteria.addToCriteria(Restrictions.sqlRestriction(
                            "'empty_in_criteria_for_$property' = ''"))
                }
            } else {
                eq property, values
            }
        }
    }
}
