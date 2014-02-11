package org.transmartproject.core.ontology

/**
 * A study (or trial) represents a unit of patients and data for these patients.
 */
public interface Study {

    /**
     * The ontology term object associated with this object
     * @return the ontology term
     */
    OntologyTerm getOntologyTerm()
}
