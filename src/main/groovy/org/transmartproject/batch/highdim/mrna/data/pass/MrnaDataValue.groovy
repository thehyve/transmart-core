package org.transmartproject.batch.highdim.mrna.data.pass

import groovy.transform.Canonical
import org.transmartproject.batch.patient.Patient

/**
 * Single value for a (patient, probe) pair.
 */
@Canonical
class MrnaDataValue {
    String probe
    Patient patient
    Double value
}
