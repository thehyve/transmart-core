package org.transmartproject.core.multidimquery.counts

import spock.lang.Specification

import static org.transmartproject.core.multidimquery.counts.Counts.BELOW_THRESHOLD
import static org.transmartproject.core.multidimquery.counts.Counts.UNKNOWN

class CountsSpec extends Specification {

    def 'adding below threshold counts to exact counts'() {
        def belowThresholdCounts = new Counts(patientCount: BELOW_THRESHOLD, observationCount: BELOW_THRESHOLD)
        def exactCounts = new Counts(patientCount: 10, observationCount: 20)

        when: 'we add below threshold counts to exact counts'
        def result = belowThresholdCounts.plus(exactCounts)

        then: 'the result contains unknown counts'
        result.patientCount == UNKNOWN
        result.observationCount == UNKNOWN

        when: 'we add exact counts to below threshold counts'
        def result2 = exactCounts.plus(belowThresholdCounts)

        then: 'the result contains unknown counts'
        result2.patientCount == UNKNOWN
        result2.observationCount == UNKNOWN
    }

    def 'sum of two below threshold counts'() {
        def belowThresholdCounts = new Counts(patientCount: BELOW_THRESHOLD, observationCount: BELOW_THRESHOLD)

        when: 'we add up below threshold counts'
        def result = belowThresholdCounts.plus(belowThresholdCounts)

        then: 'the result contains unknown counts'
        result.patientCount == UNKNOWN
        result.observationCount == UNKNOWN
    }

    def 'sum of two unknown counts'() {
        def unknownCounts = new Counts(patientCount: UNKNOWN, observationCount: UNKNOWN)

        when: 'we add up unknown counts'
        def result = unknownCounts.plus(unknownCounts)

        then: 'the result contains unknown counts'
        result.patientCount == UNKNOWN
        result.observationCount == UNKNOWN
    }

    def 'merge unknown counts with below threshold counts'() {
        def unknownCounts = new Counts(patientCount: UNKNOWN, observationCount: UNKNOWN)
        def belowThresholdCounts = new Counts(patientCount: BELOW_THRESHOLD, observationCount: BELOW_THRESHOLD)

        when: 'we merge unknown counts into below threshold ones'
        belowThresholdCounts.merge(unknownCounts)

        then: 'the counts equals to unknown value'
        belowThresholdCounts.patientCount == UNKNOWN
        belowThresholdCounts.observationCount == UNKNOWN
    }
}
