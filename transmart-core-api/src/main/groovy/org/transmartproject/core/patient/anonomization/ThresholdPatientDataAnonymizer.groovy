package org.transmartproject.core.patient.anonomization

import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.ontology.MDStudy
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.core.users.User

import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS_WITH_THRESHOLD
import static org.transmartproject.core.users.PatientDataAccessLevel.SUMMARY

/**
 * Use threshold field below which does not show patient data.
 * {@link this.BELOW_THRESHOLD_COUNT} used for both patient and observation count instead of original values then.
 */
class ThresholdPatientDataAnonymizer implements PatientDataAnonymizer {

    public static final long BELOW_THRESHOLD_COUNT = -2
    long patientCountsThreshold = 0
    private final AuthorisationChecks authorisationChecks

    ThresholdPatientDataAnonymizer(AuthorisationChecks authorisationChecks) {
        this.authorisationChecks = authorisationChecks
    }

    @Override
    Counts toPatientNonIdentifiableCountsIfNeeded(Counts originalCounts, User user, MDStudy study) {
        if (authorisationChecks.canReadPatientData(user, SUMMARY, study)) {
            return originalCounts
        }
        if (authorisationChecks.canReadPatientData(user, COUNTS_WITH_THRESHOLD, study)) {
            if (originalCounts.patientCount < getPatientCountsThreshold()) {
                return new Counts(
                        patientCount: BELOW_THRESHOLD_COUNT,
                        observationCount: BELOW_THRESHOLD_COUNT
                )
            }
            return originalCounts
        }
        throw new AccessDeniedException("User ${user.username} can't access ${study.name} study subject data.")
    }
}
