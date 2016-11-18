package tests.rest.v2.protobuf

import base.RESTSpec
import protobuf.ObservationsMessageProto

import selectors.protobuf.ObservationSelector
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.constraints.*

class GetMultipleObservationsSpec extends RESTSpec{

    def "proto test"(){
        given: "study EHR is loaded"

        when: "for that study I Aggregated the concept Heart Rate with type max"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: PatientSetConstraint, patientSetId: 0, patientIds: [-62,-42]],
                        [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"]
                ]
        ]

        ObservationsMessageProto responseData = getProtobuf("query/hypercube", toQuery(constraintMap))

        then: "the number 102 is returned"
        ObservationSelector selector = new ObservationSelector(responseData)

        assert selector.selectStringValue(0, "ConceptDimension", "conceptCode").equals('EHR:VSIGN:HR')
    }
}
