package org.transmartproject.core.dataquery.clinical

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.doc.Experimental
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.OntologyTerm

@Experimental
interface PatientsResource {

    /**
     * Fetch a patient by patient number.
     *
     * @param id the patient number
     * @return the patient
     * @throws NoSuchResourceException if there's no user with such id
     */
    Patient getPatientById(Long id) throws NoSuchResourceException

    /**
     * Fetches all the patients that have at least one observation for the given OntologyTerm.
     *
     * @param term to fetch patients for
     * @return list of patients
     */
    List<Patient> getPatients(OntologyTerm term);

}
