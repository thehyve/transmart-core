package org.transmartproject.ontology

/**
 *
 */
interface ExternalOntologyTermService {

    /**
     * Initialise the service with the parameters in <code>parameters</code>.
     * Which parameters are supported depends on the service type.
     * @param params
     */
    public void init(Map parameters)

    /**
     * Fetches {@link OntologyMap} objects for the variable, based on the
     * provided category code and data label.
     * Some services is requested to provide a canonical ontology code and
     * label for the term and the codes of its ancestors.
     * The result should contain an entry for the variable, which also
     * contains the category code and data label, and an entry for every
     * ancestor, without category code or data label.
     *
     * @param categoryCode
     * @param dataLabel
     */
    public List<OntologyMap> fetchPreferredConcept(String categoryCode, String dataLabel)

}