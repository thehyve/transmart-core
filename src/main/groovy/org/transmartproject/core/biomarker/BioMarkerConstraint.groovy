package org.transmartproject.core.biomarker

/**
 * Limits the results of {@link BioMarkersResource#retrieveBioMarkers(List<BioMarkerConstraint>)}
 */
interface BioMarkerConstraint {

    /**
     * Constraint that limits the bio markers in the result to those that have
     * a specific value for a certain property.
     *
     * Parameters: propertyName => (id|name|description|organism|
     *                              primary source code|primary external id|type)
     *             value => <string value>
     */
    public final static String BIO_MARKER_PROPERTY_CONSTRAINT = 'bioMarkerProperty';
}
