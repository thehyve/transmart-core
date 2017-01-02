package tests.rest.v2.json

import base.RESTSpec
import selectors.ObservationSelectorJson
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

        when: "I get observations for patient 1 for concept HEARTRATE"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: PatientSetConstraint, patientIds: -62],
                        [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]
                ]
        ]

        def responseData = get(PATH_OBSERVATIONS, contentTypeForJSON, toQuery(constraintMap))
        ObservationSelectorJson selector = new ObservationSelectorJson(parseHypercube(responseData))

        then: "3 observations are returned"

        assert selector.cellCount == 3
        (0..<selector.cellCount).each {
            assert selector.select(it, "ConceptDimension", "conceptCode", 'String').equals('EHR:VSIGN:HR')
            assert selector.select(it, "PatientDimension", "age", 'Int') == 30
            assert [60, 59, 80].contains(selector.select(it) as int)
        }
    }

    /**
     *  given: "EHR is loaded"
     *  when: "I get all observations of that studie"
     *  then: "7 observations have a valid startDate as JSON date
     */
    def "Start time of observations are exposed through REST API"(){
        given: "EHR is loaded"

        when: "I get all observations of that studie"
        def constraintMap = [type: StudyNameConstraint, studyId: EHR_ID]

        def responseData = get(PATH_OBSERVATIONS, contentTypeForJSON, toQuery(constraintMap))
        ObservationSelectorJson selector = new ObservationSelectorJson(parseHypercube(responseData))
        (0..<selector.cellCount).each {
            println "TYPE: ${selector.select(it, 'StartTimeDimension', null, 'Date')?.class?.simpleName}"
        }

        then: "7 observations have a valid startDate, all formated with a datestring"

        int validStartDate = 0
        (0..<selector.cellCount).each {
            if (selector.select(it, 'StartTimeDimension', null, 'Date') != null){
                validStartDate++
                assert (selector.select(it, 'StartTimeDimension', null, 'Date') as Date) instanceof Date
            }
            assert (selector.select(it, "ConceptDimension", "conceptCode", 'String') == 'EHR:VSIGN:HR' ||
                    selector.select(it, "ConceptDimension", "conceptCode", 'String') == 'EHR:DEM:AGE')
        }
        assert validStartDate == 7
    }

    /**
     *  given: "EHR is loaded"
     *  when: "I get all observations of that studie"
     *  then: "4 observations have a nonNUll endDate as JSON date
     */
    def "end time of observations are exposed through REST API"(){
        given: "EHR is loaded"

        when: "I get all observations of that studie"
        def constraintMap = [type: StudyNameConstraint, studyId: EHR_ID]
        def responseData = get(PATH_OBSERVATIONS, contentTypeForJSON, toQuery(constraintMap))
        ObservationSelectorJson selector = new ObservationSelectorJson(parseHypercube(responseData))

        then: "4 observations have a nonNUll endDate, all formated with a datestring"

        int nonNUllEndDate = 0
        (0..<selector.cellCount).each {
            if (selector.select(it, 'EndTimeDimension', null, 'Date') != null){
                nonNUllEndDate++
                assert (selector.select(it, 'EndTimeDimension', null, 'Date') as Date) instanceof Date
            }
            assert (selector.select(it, "ConceptDimension", "conceptCode", 'String') == 'EHR:VSIGN:HR' ||
                    selector.select(it, "ConceptDimension", "conceptCode", 'String') == 'EHR:DEM:AGE')
        }
        assert nonNUllEndDate == 4
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

        when: "for 2 patients I request the concept Heart Rate"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: PatientSetConstraint, patientIds: [-62,-42]],
                        [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]
                ]
        ]
        def responseData = get(PATH_OBSERVATIONS, contentTypeForJSON, toQuery(constraintMap))
        ObservationSelectorJson selector = new ObservationSelectorJson(parseHypercube(responseData))

        then: "6 obsevations are returned"

        assert selector.cellCount == 6
        (0..<selector.cellCount).each {
            assert selector.select(it, "ConceptDimension", "conceptCode", 'String').equals('EHR:VSIGN:HR')
            assert selector.select(it, "PatientDimension", "age", 'Int') == (it < 3 ? 30 : 52)
            assert [60, 59, 80, 78, 56, 102].contains(selector.select(it) as int)
        }
    }
}
