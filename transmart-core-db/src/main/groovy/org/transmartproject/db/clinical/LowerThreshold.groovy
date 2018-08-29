package org.transmartproject.db.clinical

import grails.util.Holders
import groovy.transform.CompileStatic
import org.springframework.stereotype.Component
import org.transmartproject.core.multidimquery.counts.Counts
import org.transmartproject.core.multidimquery.crosstable.CrossTable

@Component
@CompileStatic
class LowerThreshold {

    static final long BELOW_THRESHOLD_VALUE = -2
    static final Counts BELOW_THRESHOLD_COUNTS = new Counts(BELOW_THRESHOLD_VALUE, BELOW_THRESHOLD_VALUE)

    private Long patientCountThreshold

    Long getPatientCountThreshold() {
        if (patientCountThreshold == null) {
            patientCountThreshold = Holders.config.getProperty('patientCountThreshold', Long, 0L)
        }
        patientCountThreshold
    }

    // For testing purposes
    void setPatientCountThreshold(long value) {
        patientCountThreshold = value
    }

    boolean isHigherThan(Counts counts) {
        isHigherThan(counts.patientCount)
    }

    boolean isHigherThan(Long patientCount) {
        patientCount < patientCountThreshold
    }

    boolean isHigherThanAnyOf(CrossTable crossTable) {
        crossTable.rows.parallelStream().anyMatch({
            it.parallelStream().anyMatch({ isHigherThan(it) })
        })
    }

    boolean isHigherThanAnyOf(Map<String, Counts> counts) {
        counts.values().parallelStream().anyMatch({ isHigherThan(it) })
    }

}
