package tests.rest.v2.protobuf

import base.RESTSpec
import protobuf.ObservationsMessageProto
import selectors.protobuf.ObservationSelector
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

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "3 observations are returned"
        ObservationSelector selector = new ObservationSelector(responseData)

        assert selector.cellCount == 3
        (0..<selector.cellCount).each {
            assert selector.select(it, "ConceptDimension", "conceptCode", 'String').equals('EHR:VSIGN:HR')
            assert selector.select(it, "PatientDimension", "age", 'Int') == 30
            assert [60.0D, 59.0D, 80.0D].contains(selector.select(it))
        }
    }

    /**
     *  given: "EHR is loaded"
     *  when: "I get all observations of that studie"
     *  then: "7 observations have a valid startDate as timestamp value
     */
//    @IgnoreIf({SUPPRESS_KNOWN_BUGS}) //FIXME: TMPDEV-125 protobuf sterilization, DATE fields missing from dimensions
    def "Start time of observations are exposed through REST API"(){
        given: "EHR is loaded"

        when: "I get all observations of that studie"
        def constraintMap = [type: StudyNameConstraint, studyId: EHR_ID]

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "7 observations have a valid startDate, all formated with a datestring"
        ObservationSelector selector = new ObservationSelector(responseData)


        int validStartDate = 0
        (0..<selector.cellCount).each {
            if (selector.select(it, 'StartTimeDimension', null, 'Timestamp') != null){
                validStartDate++
                assert selector.select(it, 'StartTimeDimension', null, 'Timestamp') instanceof Number
            }
            assert (selector.select(it, "ConceptDimension", "conceptCode", 'String') == 'EHR:VSIGN:HR' ||
                    selector.select(it, "ConceptDimension", "conceptCode", 'String') == 'EHR:DEM:AGE')
        }
        assert validStartDate == 7
    }

    /**
     *  given: "EHR is loaded"
     *  when: "I get all observations of that studie"
     *  then: "4 observations have a nonNUll endDate as timestamp value
     */
//    @IgnoreIf({SUPPRESS_KNOWN_BUGS}) //FIXME: TMPDEV-125 protobuf sterilization, DATE fields missing from dimensions
    def "end time of observations are exposed through REST API"(){
        given: "EHR is loaded"

        when: "I get all observations of that studie"
        def constraintMap = [type: StudyNameConstraint, studyId: EHR_ID]

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "4 observations have a nonNUll endDate, all formated with a datestring"
        ObservationSelector selector = new ObservationSelector(responseData)

        int nonNUllEndDate = 0
        (0..<selector.cellCount).each {
            if (selector.select(it, 'EndTimeDimension', null, 'Timestamp') != null){
                nonNUllEndDate++
                assert selector.select(it, 'EndTimeDimension', null, 'Timestamp') instanceof Number
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

        ObservationsMessageProto responseData = getProtobuf(PATH_OBSERVATIONS, toQuery(constraintMap))

        then: "6 obsevations are returned"
        ObservationSelector selector = new ObservationSelector(responseData)

        assert selector.cellCount == 6
        (0..<selector.cellCount).each {
            assert selector.select(it, "ConceptDimension", "conceptCode", 'String').equals('EHR:VSIGN:HR')
            assert selector.select(it, "PatientDimension", "age", 'Int') == (it < 3 ? 30 : 52)
            assert [60.0D, 59.0D, 80.0D, 78.0D, 56.0D, 102.0D].contains(selector.select(it))
        }
    }
}
