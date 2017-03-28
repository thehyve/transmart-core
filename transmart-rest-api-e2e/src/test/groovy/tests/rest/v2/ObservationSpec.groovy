/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.contentTypeForJSON
import static base.ContentTypeFor.contentTypeForProtobuf
import static config.Config.*
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.Operator.OR
import static tests.rest.v2.Operator.EQUALS
import static tests.rest.v2.Operator.LESS_THAN
import static tests.rest.v2.ValueType.NUMERIC
import static tests.rest.v2.ValueType.STRING
import static tests.rest.v2.constraints.*

class ObservationSpec extends RESTSpec {

/**
 *  given: "study EHR is loaded"
 *  when: "for that study I get all observations for a heart rate"
 *  then: "9 observations are returned"
 */
    @RequiresStudy(EHR_ID)
    def "get observations using POST method"() {

        given: "study EHR is loaded"
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: contentTypeForJSON,
                body      : [
                        constraint: toJSON([
                                type: ConceptConstraint,
                                path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                        ]),
                        type      : 'clinical'
                ]
        ]

        when: "for that study I get all observations for a heart rate"
        def responseData = post(request)

        then: "9 observations are returned"
        assert responseData.cells.size() == 9
    }
}
