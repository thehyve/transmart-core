/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static base.ContentTypeFor.PROTOBUF
import static config.Config.*
import static tests.rest.Operator.AND
import static tests.rest.Operator.LESS_THAN
import static tests.rest.ValueType.NUMERIC
import static tests.rest.constraints.*

/**
 * these test are here to test the correct handling of the max number of sub queries on oracle.
 */
@RequiresStudy(ORACLE_1000_PATIENT_ID)
class BigStudySpec extends RESTSpec {

    def "get 1000 patients"() {
        def request = [
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
                                [type: StudyNameConstraint, studyId: ORACLE_1000_PATIENT_ID],
                                [type    : FieldConstraint,
                                 field   : [dimension: 'patient',
                                            fieldName: 'age',
                                            type     : NUMERIC],
                                 operator: LESS_THAN,
                                 value   : 70]
                        ]
                ]),
                statusCode: 200
        ]


        when:

        def responseData = get(request)

        then:
        println(responseData.patients.size())
        assert responseData.patients.size() > 1000

    }

    def "get observations for 1000 patients"() {

        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: acceptType,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
                                [type: StudyNameConstraint, studyId: ORACLE_1000_PATIENT_ID],
                                [type    : FieldConstraint,
                                 field   : [dimension: 'patient',
                                            fieldName: 'age',
                                            type     : NUMERIC],
                                 operator: LESS_THAN,
                                 value   : 70]
                        ]
                ]),
                statusCode: 200
        ]


        when:

        def responseData = get(request)
        def selector = newSelector(responseData)

        then:
        assert selector.cellCount > 100000
        println(selector.cellCount)

        where:
        acceptType | newSelector
        JSON       | jsonSelector
        PROTOBUF   | protobufSelector
    }

}
