package org.transmartproject.core.ontology

/**
 * Created by ewelina on 7-12-16.
 */
interface ExternalOntologyTerm {
    /**
     * Referencing terms from external ontology servers
     *
     * @return preferred codes for the given concept code
     */
    Object fetchPreferredConcept(String conceptCode)
}