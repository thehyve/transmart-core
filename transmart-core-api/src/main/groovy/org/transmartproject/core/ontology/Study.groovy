package org.transmartproject.core.ontology

import org.transmartproject.core.dataquery.Patient

/**
 * A study (or trial) represents a unit of patients and data for these patients.
 */
public interface Study {

    /**
     * The (hopefully unique) name of the study.
     *
     * @return the id of the study (it also appears as trial in the system)
     */
    String getId()

    /**
     * The ontology term object associated with this object.
     * @return the ontology term
     */
    OntologyTerm getOntologyTerm()

    /**
     * All the patients belonging to this study.
     * @return the patients for this study
     */
    Set<Patient> getPatients()
}
