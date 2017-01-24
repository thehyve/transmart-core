package tests.rest.v2.hypercube

import base.RESTSpec
import spock.lang.Requires

import static config.Config.*
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.constraints.*

@Requires({EHR_LOADED})
class MultipleObservationsSpec extends RESTSpec{

    /**
     *  Given: "ClinicalTrial is loaded"
     *  When: "I get observations for patient 1 for concept HEARTRATE"
     *  Then: "3 observations are returned"
     */
    def "Multiple observations are possible per combination of concept and patient"(){
        given: "Ward-ClinicalTrial is loaded"
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: toQuery([
                        type: Combination,
                        operator: AND,
                        args: [
                                [type: PatientSetConstraint, patientIds: -62],
                                [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]
                        ]
                ])
        ]

        when: "I get observations for patient 1 for concept HEARTRATE"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "3 observations are returned"
        assert selector.cellCount == 3
        (0..<selector.cellCount).each {
            assert selector.select(it, "concept", "conceptCode", 'String').equals('EHR:VSIGN:HR')
            assert selector.select(it, "patient", "age", 'Int') == 30
            assert [60, 59, 80].contains(selector.select(it) as int)
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

    /**
     *  given: "EHR is loaded"
     *  when: "I get all observations of that studie"
     *  then: "7 observations have a valid startDate as JSON date
     */
    def "Start time of observations are exposed through REST API"(){
        given: "EHR is loaded"
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: toQuery([type: StudyNameConstraint, studyId: EHR_ID])
        ]

        when: "I get all observations of that studie"
        def responseData = get(request)

        def selector = newSelector(responseData)
        (0..<selector.cellCount).each {
            println "TYPE: ${selector.select(it, 'start time', null, 'Timestamp')?.class?.simpleName}"
        }

        then: "7 observations have a valid startDate, all formated with a datestring"

        int validStartDate = 0
        (0..<selector.cellCount).each {
            if (selector.select(it, 'start time', null, 'Timestamp') != null){
                validStartDate++
                assertion(selector, it)
            }
            assert (selector.select(it, "concept", "conceptCode", 'String') == 'EHR:VSIGN:HR' ||
                    selector.select(it, "concept", "conceptCode", 'String') == 'EHR:DEM:AGE')
        }
        assert validStartDate == 7

        where:
        acceptType | newSelector | assertion
        contentTypeForJSON | jsonSelector | {it, index -> assert (it.select(index, 'start time', null, 'Timestamp') as Date) instanceof Date}
        contentTypeForProtobuf | protobufSelector | {it, index -> assert it.select(index, 'start time', null, 'Timestamp') instanceof Number}
    }

    /**
     *  given: "EHR is loaded"
     *  when: "I get all observations of that studie"
     *  then: "4 observations have a nonNUll endDate as JSON date
     */
    def "end time of observations are exposed through REST API"(){
        given: "EHR is loaded"
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: toQuery([type: StudyNameConstraint, studyId: EHR_ID])
        ]

        when: "I get all observations of that studie"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "4 observations have a nonNUll endDate, all formated with a datestring"

        int nonNUllEndDate = 0
        (0..<selector.cellCount).each {
            if (selector.select(it, 'end time', null, 'Timestamp') != null){
                nonNUllEndDate++
                assertion(selector, it)
            }
            assert ['EHR:DEM:AGE', 'EHR:VSIGN:HR'].contains(selector.select(it, "concept", "conceptCode", 'String'))
        }
        assert nonNUllEndDate == 4

        where:
        acceptType | newSelector | assertion
        contentTypeForJSON | jsonSelector | {it, index -> assert (it.select(index, 'start time', null, 'Timestamp') as Date) instanceof Date}
        contentTypeForProtobuf | protobufSelector | {it, index -> assert it.select(index, 'start time', null, 'Timestamp') instanceof Number}
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "for 2 patients I request the concept Heart Rate"
     *  then: "6 obsevations are returned"
     *
     * @return
     */
    def "get MultipleObservations per user"(){
        given: "study EHR is loaded"
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: toQuery([
                        type: Combination,
                        operator: AND,
                        args: [
                                [type: PatientSetConstraint, patientIds: [-62,-42]],
                                [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]
                        ]
                ])
        ]

        when: "for 2 patients I request the concept Heart Rate"
        def responseData = get(request)
        def selector = newSelector(responseData)

        then: "6 obsevations are returned"

        assert selector.cellCount == 6
        (0..<selector.cellCount).each {
            assert selector.select(it, "concept", "conceptCode", 'String').equals('EHR:VSIGN:HR')
            assert selector.select(it, "patient", "age", 'Int') == (it < 3 ? 30 : 52)
            assert [60, 59, 80, 78, 56, 102].contains(selector.select(it) as int)
        }

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }
}
