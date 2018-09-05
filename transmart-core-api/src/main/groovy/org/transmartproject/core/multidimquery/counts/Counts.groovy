package org.transmartproject.core.multidimquery.counts

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class Counts {
    /**
     * Number of observations. -1 when is not calculated. -2 when patient count is -2.
     */
    long observationCount
    /**
     * Number of patients. Could be -2 when the patient count is below the threshold.
     */
    long patientCount

    //TODO -2 test case
    Counts plus(Counts other) {
        new Counts(observationCount + other.observationCount, patientCount + other.patientCount)
    }

    void merge(Counts other) {
        observationCount += other.observationCount
        patientCount += other.patientCount
    }
}
