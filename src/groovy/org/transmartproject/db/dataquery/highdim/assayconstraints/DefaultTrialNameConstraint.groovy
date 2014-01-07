package org.transmartproject.db.dataquery.highdim.assayconstraints

import grails.orm.HibernateCriteriaBuilder
import groovy.transform.Canonical
import org.transmartproject.core.exceptions.InvalidRequestException

@Canonical
class DefaultTrialNameConstraint extends AbstractAssayConstraint {

    String trialName

    @Override
    void addConstraintsToCriteria(HibernateCriteriaBuilder builder) throws InvalidRequestException {
        /** @see org.transmartproject.db.dataquery.highdim.DeSubjectSampleMapping */
        builder.with {
            eq 'trialName', trialName
        }
    }
}
