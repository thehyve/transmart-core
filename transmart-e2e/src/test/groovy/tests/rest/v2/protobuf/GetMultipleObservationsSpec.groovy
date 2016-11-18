package tests.rest.v2.protobuf

import base.RESTSpec
import protobuf.ObservationsMessageProto

import selectors.protobuf.ObservationSelector
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.constraints.*

class GetMultipleObservationsSpec extends RESTSpec{

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
                        [type: PatientSetConstraint, patientSetId: 0, patientIds: [-62,-42]],
                        [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]
                ]
        ]

        ObservationsMessageProto responseData = getProtobuf("query/hypercube", toQuery(constraintMap))

        then: "6 obsevations are returned"
        ObservationSelector selector = new ObservationSelector(responseData)

        assert selector.cellCount == 6
        (0..<selector.cellCount).each {
            assert selector.select(it, "ConceptDimension", "conceptCode", 'String').equals('EHR:VSIGN:HR')
            assert selector.select(it, "PatientDimension", "age", 'Int') == (it < 3 ? 30 : 52)
            assert selector.select(it) == [80.0, 59.0, 60.0, 102.0, 56.0, 78.0].get(it)
        }
    }
}
