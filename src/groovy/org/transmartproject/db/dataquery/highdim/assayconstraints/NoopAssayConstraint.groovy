package org.transmartproject.db.dataquery.highdim.assayconstraints

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.core.exceptions.InvalidRequestException

class NoopAssayConstraint extends AbstractAssayConstraint {

    @Override
    void addConstraintsToCriteria(HibernateCriteriaBuilder builder) throws InvalidRequestException {
        // purposefully left empty
    }
}
