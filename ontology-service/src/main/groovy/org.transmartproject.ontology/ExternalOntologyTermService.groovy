package org.transmartproject.ontology

/**
 *
 */
interface ExternalOntologyTermService {

    /**
     *
     * @param categoryCode
     * @param dataLabel
     * @return
     */
    public List<OntologyMap> fetchPreferredConcept(String categoryCode, String dataLabel)

}