package org.transmartproject.core.patient.anonomization

import org.transmartproject.core.multidimquery.counts.Counts

/**
 * Use threshold below which does not show patient data.
 */
abstract class AbstractThresholdPatientDataAnonymizer implements PatientDataAnonymizer {

    @Override
    Counts toPatientNonIdentifiableCounts(Counts originalCounts, Integer patientCountsThreshold) {
        if (originalCounts.patientCount <= patientCountsThreshold) {
            return new Counts(
                    patientCount: getNonIdentifiablePatientCount(originalCounts.patientCount),
                    observationCount: getNonIdentifiableObservationCount(originalCounts.observationCount)
            )
        } else {
            return originalCounts
        }
    }

    protected abstract long getNonIdentifiablePatientCount(long originalPatientCount)

    protected abstract long getNonIdentifiableObservationCount(long originalObservationCount)

}
