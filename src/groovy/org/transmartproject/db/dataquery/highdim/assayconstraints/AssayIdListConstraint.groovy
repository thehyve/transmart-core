package org.transmartproject.db.dataquery.highdim.assayconstraints

import grails.orm.HibernateCriteriaBuilder
import grails.validation.Validateable
import org.transmartproject.core.exceptions.InvalidRequestException

@Validateable
class AssayIdListConstraint extends AbstractAssayConstraint {

    List<Long> ids

    @Override
    void addConstraintsToCriteria(HibernateCriteriaBuilder builder) throws InvalidRequestException {
        /** @see org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping */
        builder.with {
            'in' 'id', ids
        }
    }
}
