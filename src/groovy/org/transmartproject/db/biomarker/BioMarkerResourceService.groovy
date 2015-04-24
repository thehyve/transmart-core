package org.transmartproject.db.biomarker

import org.springframework.stereotype.Component
import org.transmartproject.core.biomarker.BioMarkerConstraint
import org.transmartproject.core.biomarker.BioMarkerResource
import org.transmartproject.core.biomarker.BioMarkerResult
import org.transmartproject.core.exceptions.InvalidArgumentsException

@Component
class BioMarkerResourceService implements BioMarkerResource {

    @Override
    BioMarkerResult retrieveBioMarkers(List<BioMarkerConstraint> constraints) {
        def criteria = BioMarkerCoreDb.createCriteria()
        criteria.createCriteriaInstance()
        constraints.each { BioMarkerCriteriaConstraint c ->
            c.doWithCriteriaBuilder(criteria)
        }
        new BioMarkerScrollableResultsWrappingIterable(criteria.scroll())
    }

    @Override
    BioMarkerConstraint createConstraint(Map<String, Object> parameters, String name) {
        switch (name) {
            case BioMarkerConstraint.CORRELATED_BIO_MARKERS_CONSTRAINT:
                new CorrelatedBioMarkersConstraint(parameters: parameters)
                break
            case BioMarkerConstraint.PROPERTIES_CONSTRAINT:
                new BioMarkerPropertiesConstraint(parameters: parameters)
                break
            default:
                throw new InvalidArgumentsException("BioMarkerConstraint of type ${name} does not exist.")
        }
    }
}
