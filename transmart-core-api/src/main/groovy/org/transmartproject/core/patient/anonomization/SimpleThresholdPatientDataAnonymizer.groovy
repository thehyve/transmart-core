package org.transmartproject.core.patient.anonomization


/**
 * Use threshold field below which does not show patient data.
 * @see this.lessThenThresholdCountValue used for both patient and observation count instead of original values then.
 */
class SimpleThresholdPatientDataAnonymizer extends AbstractThresholdPatientDataAnonymizer {

    long lessThenThresholdCountValue = -2

    protected long getNonIdentifiablePatientCount(long originalPatientCount) {
        lessThenThresholdCountValue
    }

    protected long getNonIdentifiableObservationCount(long originalObservationCount) {
        lessThenThresholdCountValue
    }

}
