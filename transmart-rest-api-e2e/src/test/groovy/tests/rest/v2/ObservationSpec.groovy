/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.EHR_ID
import static config.Config.PATH_OBSERVATIONS
import static org.springframework.http.HttpMethod.GET
import static org.springframework.http.HttpMethod.POST
import static tests.rest.constraints.ConceptConstraint

class ObservationSpec extends RESTSpec {

    /**
     *  given: "study EHR is loaded"
     *  when: "for that study I get all observations for a heart rate"
     *  then: "9 observations are returned"
     */
    @RequiresStudy(EHR_ID)
    def "get observations"() {

        given: "study EHR is loaded"
        def params = [
                constraint: [
                        type: ConceptConstraint,
                        path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                ],
                type      : 'clinical'
        ]
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: JSON
        ]

        when: "for that study I get all observations for a heart rate"
        def responseData = getOrPostRequest(method, request, params)

        then: "9 observations are returned"
        assert responseData.cells.size() == 9

        where:
        method  | _
        POST    | _
        GET     | _
    }

}
