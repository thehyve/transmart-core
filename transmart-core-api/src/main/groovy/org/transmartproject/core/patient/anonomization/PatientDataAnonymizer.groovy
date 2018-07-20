package org.transmartproject.core.patient.anonomization

import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.User

/**
 * Service to obfucate and anonomize patient data.
 */
interface PatientDataAnonymizer {

    Counts toPatientNonIdentifiableCountsIfNeeded(Counts originalCounts, User user, MDStudy study)

}
