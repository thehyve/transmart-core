package tests.rest.v2

import base.RESTSpec

import static config.Config.*
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.Operator.LESS_THAN
import static tests.rest.v2.ValueType.NUMERIC
import static tests.rest.v2.constraints.*


/**
 * these test are here to test the correct handling of the max number of sub queries on oracle.
 */
class BigStudySpec extends RESTSpec{

    def "get 1000 patients"(){
        def request = [
                path: PATH_PATIENTS,
                acceptType: contentTypeForJSON,
                query: toQuery([
                        type: Combination,
                        operator: AND,
                        args: [
                                [type: StudyNameConstraint, studyId: Oracle_1000_Patient_ID],
                                [type: FieldConstraint,
                                 field: [dimension: 'patient',
                                         fieldName: 'age',
                                         type: NUMERIC ],
                                 operator: LESS_THAN,
                                 value:70]
                        ]
                ]),
                statusCode: 200
        ]


        when:

        def responseData = get(request)

        then:
        assert responseData.patients > 1000

    }

    def "get observations for 1000 patients"(){
        def request = [
                path: PATH_OBSERVATIONS,
                acceptType: acceptType,
                query: toQuery([
                        type: Combination,
                        operator: AND,
                        args: [
                                [type: StudyNameConstraint, studyId: Oracle_1000_Patient_ID],
                                [type: FieldConstraint,
                                 field: [dimension: 'patient',
                                         fieldName: 'age',
                                         type: NUMERIC ],
                                 operator: LESS_THAN,
                                 value:70]
                        ]
                ]),
                statusCode: 200
        ]


        when:

        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        assert selector.cellCount > 1000

        where:
        acceptType | newSelector
        contentTypeForJSON | jsonSelector
        contentTypeForProtobuf | protobufSelector
    }

}
