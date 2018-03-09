package org.transmartproject.core.dataquery.clinical

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.exceptions.NoSuchResourceException

interface PatientsResource {

    /**
     * Fetch a patient by patient number.
     *
     * @param id the patient number
     * @return the patient
     * @throws NoSuchResourceException if there's no user with such id
     */
    Patient getPatientById(Long id) throws NoSuchResourceException

}
