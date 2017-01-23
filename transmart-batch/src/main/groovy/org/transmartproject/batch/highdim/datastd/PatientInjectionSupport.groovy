package org.transmartproject.batch.highdim.datastd

import org.transmartproject.batch.patient.Patient

/**
 * Interface for beans that hold patient
 */
interface PatientInjectionSupport {
    void setPatient(Patient patient)

    String getSampleCode()
}
