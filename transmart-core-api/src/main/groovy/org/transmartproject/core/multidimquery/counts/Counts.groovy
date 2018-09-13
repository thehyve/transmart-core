package org.transmartproject.core.multidimquery.counts

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class Counts {

    public static final long UNKNOWN = -1
    public static final long BELOW_THRESHOLD = -2
    /**
     * Number of observations. -1 when is not known. -2 when patient count is -2.
     */
    long observationCount
    /**
     * Number of patients. -1 when is not known. Could be -2 when the patient count is below the threshold.
     */
    long patientCount

    Counts plus(Counts other) {
        long observationCountSum
        if (observationCount < 0 || other.observationCount < 0) {
            observationCountSum = UNKNOWN
        } else {
            observationCountSum = observationCount + other.observationCount
        }
        long patientCountSum
        if (patientCount < 0 || other.patientCount < 0) {
            patientCountSum = UNKNOWN
        } else {
            patientCountSum = patientCount + other.patientCount
        }
        new Counts(observationCountSum, patientCountSum)
    }

    void merge(Counts other) {
        def result = this.plus(other)
        this.patientCount = result.patientCount
        this.observationCount = result.observationCount
    }
}
