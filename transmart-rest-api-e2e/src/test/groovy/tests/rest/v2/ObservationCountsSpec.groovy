/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.*
import static org.springframework.http.HttpMethod.GET
import static org.springframework.http.HttpMethod.POST
import static tests.rest.constraints.ConceptConstraint

@RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
class ObservationCountsSpec extends RESTSpec {

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "I count observations for the concept Heart Rate"
     *  then: "I get a count including observations from the restricted study"
     */
    def "restricted counts"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
        def request = [
                path      : PATH_COUNTS,
                acceptType: JSON,
                query     : toQuery([type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"])
        ]

        when: "I count observations for the concept Heart Rate"
        def responseData = get(request)

        then: "I get a count excluding observations from the restricted study"
        assert responseData.observationCount == 5
        assert responseData.patientCount == 4
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
     *  when: "I count observations for the concept Heart Rate"
     *  then: "I get a count including observations from the restricted study"
     */
    def "unrestricted counts"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
        def params = [
                constraint: toJSON([type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"])
        ]
        def request = [
                path      : PATH_COUNTS,
                acceptType: JSON,
                user      : UNRESTRICTED_USER
        ]

        when: "I count observations for the concept Heart Rate"
        def responseData = getOrPostRequest(method, request, params)

        then: "I get a count including observations from the restricted study"
        assert responseData.observationCount == 7
        assert responseData.patientCount == 6

        where:
        method | _
        POST   | _
        GET    | _
    }

}
