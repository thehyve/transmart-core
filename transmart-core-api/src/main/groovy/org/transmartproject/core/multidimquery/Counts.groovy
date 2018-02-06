package org.transmartproject.core.multidimquery

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class Counts {
    long observationCount
    long patientCount

    Counts plus(Counts other) {
        new Counts(observationCount + other.observationCount, patientCount + other.patientCount)
    }

    void merge(Counts other) {
        observationCount += other.observationCount
        patientCount += other.patientCount
    }
}
