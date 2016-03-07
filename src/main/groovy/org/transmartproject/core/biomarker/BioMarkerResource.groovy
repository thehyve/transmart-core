package org.transmartproject.core.biomarker

import org.transmartproject.core.IterableResult

/**
 *
 */
interface BioMarkerResource {

    /**
     * Retrieves bio markers that satisfy given constraints.
     * @param constraints for limiting the bio markers in the result
     * @return scrollable collection of result bio markers.
     */
    IterableResult<BioMarker> retrieveBioMarkers(List<BioMarkerConstraint> constraints)

    /**
     * Instantiate certain type of constraint object.
     * @param parameters parameters essential for defining constraint.
     * @param name name of the constraint. For full list of possible names see constants in {@link BioMarkerConstraint}
     * @return Constraint instance
     */
    BioMarkerConstraint createConstraint(Map<String, Object> parameters, String name)

}
