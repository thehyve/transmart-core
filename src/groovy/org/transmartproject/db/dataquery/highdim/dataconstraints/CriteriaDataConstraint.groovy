package org.transmartproject.db.dataquery.highdim.dataconstraints

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint

interface CriteriaDataConstraint extends DataConstraint {

    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria)

}
