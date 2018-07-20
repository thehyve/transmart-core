package org.transmartproject.core.patient.anonomization

import org.transmartproject.core.multidimquery.counts.Counts

/**
 * Use threshold below which does not show patient data.
 */
abstract class AbstractThresholdPatientDataAnonymizer implements PatientDataAnonymizer {

    @Override
    Counts toPatientNonIdentifiableCounts(Counts originalCounts) {
        if (originalCounts.patientCount <= getPatientCountsThreshold()) {
            return new Counts(
                    patientCount: getNonIdentifiablePatientCount(originalCounts.patientCount),
                    observationCount: getNonIdentifiableObservationCount(originalCounts.observationCount)
            )
        } else {
            return originalCounts
        }
    }

    protected abstract long getPatientCountsThreshold()

    protected abstract long getNonIdentifiablePatientCount(long originalPatientCount)

    protected abstract long getNonIdentifiableObservationCount(long originalObservationCount)

}
