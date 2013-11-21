package org.transmartproject.db.dataquery.highdim.projections

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.core.dataquery.highdim.projections.Projection

interface CriteriaProjection<T> extends Projection<T> {

    public static final DEFAULT_REAL_PROJECTION = 'default_real_projection'

    void doWithCriteriaBuilder(HibernateCriteriaBuilder builder)

}
