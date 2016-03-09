package org.transmartproject.db.biomarker

import org.springframework.stereotype.Component
import org.transmartproject.core.IterableResult
import org.transmartproject.core.biomarker.BioMarker
import org.transmartproject.core.biomarker.BioMarkerConstraint
import org.transmartproject.core.biomarker.BioMarkerResource
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.db.util.ScrollableResultsWrappingIterable

@Component
class BioMarkerResourceService implements BioMarkerResource {

    @Override
    IterableResult<BioMarker> retrieveBioMarkers(List<BioMarkerConstraint> constraints) {
        def criteria = BioMarkerCoreDb.createCriteria()
        criteria.createCriteriaInstance()
        constraints.each { BioMarkerCriteriaConstraint c ->
            c.doWithCriteriaBuilder(criteria)
        }
        new ScrollableResultsWrappingIterable<BioMarker>(criteria.scroll())
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

    @Override
    IterableResult<String> availableTypes() {
        def result = BioMarkerCoreDb.createCriteria().scroll {
            projections {
                distinct 'type'
            }
        }

        new ScrollableResultsWrappingIterable<String>(result)
    }

    @Override
    IterableResult<String> availableOrganisms() {
        def result = BioMarkerCoreDb.createCriteria().scroll {
            projections {
                distinct 'organism'
            }
        }

        new ScrollableResultsWrappingIterable<String>(result)
    }
}
