package org.transmartproject.db.biomarker

import grails.orm.HibernateCriteriaBuilder
import org.transmartproject.core.exceptions.InvalidArgumentsException

class BioMarkerPropertiesConstraint implements BioMarkerCriteriaConstraint {

    Map<String, Object> parameters

    @Override
    void doWithCriteriaBuilder(HibernateCriteriaBuilder criteria) {
        if (!parameters) {
            throw new InvalidArgumentsException('No parameters are specified.')
        }

        criteria.and {
            parameters.each { entry ->
                if (entry.value instanceof Collection) {
                    inList(entry.key, entry.value)
                } else {
                    eq(entry.key, entry.value)
                }
            }
        }
    }
}
