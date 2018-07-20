package org.transmartproject.core.patient.anonomization

import org.transmartproject.core.multidimquery.counts.Counts

/**
 * Service to obfucate and anonomize patient data.
 */
interface PatientDataAnonymizer {

    Counts toPatientNonIdentifiableCounts(Counts originalCounts)

}
