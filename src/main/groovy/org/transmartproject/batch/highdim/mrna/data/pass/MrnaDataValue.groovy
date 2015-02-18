package org.transmartproject.batch.highdim.mrna.data.pass

import groovy.transform.Canonical
import org.transmartproject.batch.highdim.compute.DataPoint
import org.transmartproject.batch.patient.Patient

/**
 * Single value for a (patient, probe) pair.
 */
@Canonical
class MrnaDataValue implements DataPoint {
    String probe
    Patient patient
    Double value
}
