package org.transmartproject.db.config

import groovy.transform.CompileStatic
import org.transmartproject.core.config.RuntimeConfig

import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
class RuntimeConfigImpl implements RuntimeConfig {

    private AtomicInteger numberOfWorkers

    private AtomicInteger patientSetChunkSize

    RuntimeConfigImpl(int numberOfWorkers, int patientSetChunkSize) {
        this.numberOfWorkers = new AtomicInteger(numberOfWorkers)
        this.patientSetChunkSize = new AtomicInteger(patientSetChunkSize)
    }

    Integer getNumberOfWorkers() {
        return numberOfWorkers.intValue()
    }

    void setNumberOfWorkers(int numberOfWorkers) {
        this.numberOfWorkers.set(numberOfWorkers)
    }

    Integer getPatientSetChunkSize() {
        return patientSetChunkSize.intValue()
    }

    void setPatientSetChunkSize(int patientSetChunkSize) {
        this.patientSetChunkSize.set(patientSetChunkSize)
    }

}
