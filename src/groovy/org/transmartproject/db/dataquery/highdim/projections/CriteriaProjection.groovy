package org.transmartproject.db.dataquery.highdim.projections

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.core.dataquery.highdim.projections.Projection

interface CriteriaProjection<T> extends Projection<T> {

    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder)

}
