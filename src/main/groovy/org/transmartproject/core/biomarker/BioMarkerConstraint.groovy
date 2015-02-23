package org.transmartproject.core.biomarker

interface BioMarkerConstraint {

    /**
     * Limits the results of {@link BioMarkerResource#retrieveBioMarkers(List < BioMarkerConstraint >)}
     * to those that have a specific values for a certain properties.
     *
     * Properties constraints are passed as map where key represents property name and value represents the value
     * that property must be equal to. When map contains multiple properties constraints all of them must be met by
     * bio marker entry to appear in the result. In other words it's assumed there is AND logical operation
     * between property constraints.
     * e.g. [type: 'PROTEIN', organism: 'HOMO SAPIENS'] limits result to the human proteins
     *
     * If collection is passed as property value the given property of result bio marker has to match
     * to one of the supplied values
     * e.g. [type: 'GENE', name: ['TP53', 'AURKA', 'ADIRF']]
     * limits results to three specified genes.
     *
     * Property names (see {@link BioMarker}): id, type, primaryExternalId, primarySourceCode, name, description, organism
     */
    public final static String PROPERTIES_CONSTRAINT = 'propertiesConstraint';

    /**
     * Limits the results of {@link BioMarkerResource#retrieveBioMarkers(List < BioMarkerConstraint >)}
     * to those that have biological correlation of certain type (correlationName) or correlated bio marker
     * with a specific values for a certain properties (correlatedBioMarkerProperties).
     *
     * e.g. [correlationName: 'PROTEIN TO GENE', correlatedBioMarkerProperties: [ organism: 'HOMO SAPIENS' ] ]
     * limits result to bio markers (genes) that have correlation with human proteins.
     *
     * Property names:
     * - correlationName - correlation type as registered in system. e.g. 'GENE TO PROTEIN',
     * - correlatedBioMarkerProperties - map of property constraints to apply against correlated bio markers.
     *   Map structure expected to be identical to one passed with {@PROPERTIES_CONSTRAINT}
     */
    public final static String CORRELATED_BIO_MARKERS_CONSTRAINT = 'correlatedBioMarkersConstraint';

}
