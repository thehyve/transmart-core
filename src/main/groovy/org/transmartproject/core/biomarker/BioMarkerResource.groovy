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

    /**
     * Retrieves an iterable of strings of the types that are available. Useful for constraining biomarker retrieval
     * to e.g. only genes or only proteines.
     * @return scrollable collection of unique types
     */
    IterableResult<String> availableTypes()

    /**
     * Retrieves an iterable of strings of the organisms that are available.
     * @return scrollable collection of unique organisms
     */
    IterableResult<String> availableOrganisms()
}
