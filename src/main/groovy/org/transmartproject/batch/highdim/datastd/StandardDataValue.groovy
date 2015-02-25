package org.transmartproject.batch.highdim.datastd

import groovy.transform.Canonical
import org.transmartproject.batch.highdim.compute.DataPoint
import org.transmartproject.batch.patient.Patient

/**
 * Single value for a (patient, annotation) pair.
 */
@Canonical
class StandardDataValue implements DataPoint {
    String annotation
    Patient patient
    Double value
}
