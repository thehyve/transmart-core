/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2.hypercube

import annotations.RequiresStudy
import base.RESTSpec
import spock.lang.Requires

import static base.ContentTypeFor.contentTypeForJSON
import static base.ContentTypeFor.contentTypeForProtobuf
import static config.Config.*
import static tests.rest.v2.Operator.*
import static tests.rest.v2.ValueType.NUMERIC
import static tests.rest.v2.ValueType.STRING
import static tests.rest.v2.constraints.*

/**
 *  TMPREQ-17
 *      The system should allow to link one or more observations to a relative timepoint (or visit)
 *      The system should allow to retrieve observations linked to a specific relative timepoint.
 *      It should be possible to link observations from multiple concepts to the same relative timepoint. At least per study, preferably global.
 *      Relative timepoints should be able to have a linked value and unit, representing their place on the relative timescale compared to other relative timepoints with the same unit.
 */
class RelativeTimepointsSpec extends RESTSpec {

    /**
     *  given: "study CLINICAL_TRIAL is loaded"
     *  when: "I get observations from that study related to Baseline"
     *  then: "4 observations are returned"
     */
    @RequiresStudy(CLINICAL_TRIAL_ID)
    def "multiple observations to a relative timepoint"() {
        given: "study CLINICAL_TRIAL is loaded"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
                                [type: StudyNameConstraint, studyId: CLINICAL_TRIAL_ID],
                                [type    : FieldConstraint,
                                 field   : [dimension: 'trial visit',
                                            fieldName: 'relTimeLabel',
                                            type     : STRING],
                                 operator: EQUALS,
                                 value   : 'Baseline']
                        ]
                ])
        ]

        when: "I get observations related to Baseline"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "4 observations are returned"
        assert selector.cellCount == 4
        (0..<selector.cellCount).each {
            assert selector.select(it, "concept", "conceptCode", 'String').equals('CT:VSIGN:HR')
        }

        where:
        acceptType             | newSelector
        contentTypeForJSON     | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study CLINICAL_TRIAL is loaded"
     *  when: "I get observations related to week 1"
     *  and: "I get observations related to 7 days"
     *  then: "both sets of observations are the same"
     */
    @RequiresStudy(CLINICAL_TRIAL_ID)
    def "label and relative time is te same"() {
        given: "study CLINICAL_TRIAL is loaded"
        def request1week = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
                                [type: StudyNameConstraint, studyId: CLINICAL_TRIAL_ID],
                                [type    : FieldConstraint,
                                 field   : [dimension: 'trial visit',
                                            fieldName: 'relTimeLabel',
                                            type     : STRING],
                                 operator: EQUALS,
                                 value   : 'Week 1']
                        ]
                ])
        ]

        def request7days = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
                                [type: StudyNameConstraint, studyId: CLINICAL_TRIAL_ID],
                                [type    : FieldConstraint,
                                 field   : [dimension: 'trial visit',
                                            fieldName: 'relTime',
                                            type     : NUMERIC],
                                 operator: EQUALS,
                                 value   : 7]
                        ]
                ])
        ]
        when: "I get observations related to week 1"
        def responseData1Week = get(request1week)
        and: "I get observations related to 7 days"
        def responseData7Days = get(request7days)

        def selector1Week = newSelector(responseData1Week)
        def selector7Days = newSelector(responseData7Days)

        then: "both sets of observations are the same"

        assert selector1Week.cellCount == selector7Days.cellCount
        assert selector1Week.inlined.size() == selector7Days.inlined.size()
        assert selector1Week.inlined.containsAll(selector7Days.inlined)
        assert selector1Week.notInlined.size() == selector7Days.notInlined.size()
        assert selector1Week.notInlined.containsAll(selector7Days.notInlined)

        (0..<selector1Week.cellCount).each {
            assert selector1Week.select(it) == selector7Days.select(it)
        }

        where:
        acceptType             | newSelector
        contentTypeForJSON     | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "I get observations within that study related to General"
     *  then: "multiple concepts are returned"
     */
    @RequiresStudy(EHR_ID)
    def "multiple concepts to the same relative timepoint within the same study"() {
        given: "study EHR is loaded"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
                                [type: StudyNameConstraint, studyId: EHR_ID],
                                [type    : FieldConstraint,
                                 field   : [dimension: 'trial visit',
                                            fieldName: 'relTimeLabel',
                                            type     : STRING],
                                 operator: EQUALS,
                                 value   : 'General']
                        ]
                ])
        ]

        when: "I get observations related to General"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "multiple concepts are returned"
        HashSet concepts = []
        (0..<selector.cellCount).each {
            concepts.add(selector.select(it, "concept", "conceptCode", 'String'))
        }
        assert concepts.containsAll('EHR:DEM:AGE', 'EHR:VSIGN:HR')

        where:
        acceptType             | newSelector
        contentTypeForJSON     | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "studies EHR and CLINICAL_TRIAL are loaded"
     *  when: "I get observations related to the General relative time label"
     *  then: "multiple concepts from both EHR and CLINICAL_TRIAL are returned"
     */
    @RequiresStudy([CLINICAL_TRIAL_ID, EHR_ID]) @Requires({ RUN_HUGE_TESTS })
    def "multiple concepts to the same relative timepoint within several studies"() {
        given: "studies EHR and CLINICAL_TRIAL are loaded"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([type    : FieldConstraint,
                                     field   : [dimension: 'trial visit',
                                                fieldName: 'relTimeLabel',
                                                type     : STRING],
                                     operator: EQUALS,
                                     value   : 'General'])
        ]

        when: "I get observations related to the General relative time label"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "multiple concepts are returned"
        HashSet concepts = []
        (0..<selector.cellCount).each {
            concepts.add(selector.select(it, "concept", "conceptCode", 'String'))
        }
        assert concepts.containsAll('EHR:DEM:AGE', 'EHR:VSIGN:HR', 'CT:DEM:AGE')

        where:
        acceptType             | newSelector
        contentTypeForJSON     | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "study CLINICAL_TRIAL is loaded"
     *  when: "I get observations related to the last week"
     *  and: "I get observations related to GREATER_THEN the second to last week"
     *  then: "both sets of observations are the same"
     */
    @RequiresStudy(CLINICAL_TRIAL_ID)
    def "relative timescale compared to other relative timepoints"() {
        given: "study CLINICAL_TRIAL is loaded"
        def request3week = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
                                [type: StudyNameConstraint, studyId: CLINICAL_TRIAL_ID],
                                [type    : FieldConstraint,
                                 field   : [dimension: 'trial visit',
                                            fieldName: 'relTimeLabel',
                                            type     : STRING],
                                 operator: EQUALS,
                                 value   : 'Week 3']
                        ]
                ])
        ]

        def request7days = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
                                [type: StudyNameConstraint, studyId: CLINICAL_TRIAL_ID],
                                [type    : FieldConstraint,
                                 field   : [dimension: 'trial visit',
                                            fieldName: 'relTime',
                                            type     : NUMERIC],
                                 operator: GREATER_THAN,
                                 value   : 7]
                        ]
                ])
        ]
        when: "I get observations related to the last week"
        def responseData3Week = get(request3week)
        and: "I get observations related to GREATER_THEN the second to last week"
        def responseData7Days = get(request7days)

        def selector3Week = newSelector(responseData3Week)
        def selector7Days = newSelector(responseData7Days)

        then: "both sets of observations are the same"

        assert selector3Week.cellCount == selector7Days.cellCount
        assert selector3Week.inlined.size() == selector7Days.inlined.size()
        assert selector3Week.inlined.containsAll(selector7Days.inlined)
        assert selector3Week.notInlined.size() == selector7Days.notInlined.size()
        assert selector3Week.notInlined.containsAll(selector7Days.notInlined)

        (0..<selector3Week.cellCount).each {
            assert selector3Week.select(it) == selector7Days.select(it)
        }

        where:
        acceptType             | newSelector
        contentTypeForJSON     | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }
}