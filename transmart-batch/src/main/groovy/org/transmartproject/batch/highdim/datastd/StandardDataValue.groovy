package org.transmartproject.batch.highdim.datastd

import groovy.transform.Canonical
import org.transmartproject.batch.patient.Patient

/**
 * Single value for a (patient, annotation) pair.
 */
@Canonical
class StandardDataValue implements DataPoint, PatientInjectionSupport {
    String annotation
    String sampleCode
    Patient patient
    Double value
}
