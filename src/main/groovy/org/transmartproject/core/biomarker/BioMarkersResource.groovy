package org.transmartproject.core.biomarker

interface BioMarkersResource {

    BioMarkersResult retrieveBioMarkers(List<BioMarkerConstraint> constraints)

    BioMarkerConstraint createConstraint(Map<String, Object> parameters, String name)
}
