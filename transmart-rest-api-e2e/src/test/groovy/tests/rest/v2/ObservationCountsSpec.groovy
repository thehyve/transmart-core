/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2

import base.RESTSpec
import spock.lang.Requires

import static config.Config.*
import static tests.rest.v2.constraints.ConceptConstraint

class ObservationCountsSpec extends RESTSpec{

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "I count observations for the concept Heart Rate"
     *  then: "I get a count including observations from the restricted study"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "restricted count"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
        def request = [
                path: PATH_COUNTS,
                acceptType: contentTypeForJSON,
                query: toQuery([type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"])
        ]

        when: "I count observations for the concept Heart Rate"
        def responseData = get(request)

        then: "I get a count excluding observations from the restricted study"
        assert responseData.count == 5
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
     *  when: "I count observations for the concept Heart Rate"
     *  then: "I get a count including observations from the restricted study"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "unrestricted count"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)
        def request = [
                path: PATH_COUNTS,
                acceptType: contentTypeForJSON,
                query: toQuery([type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"])
        ]

        when: "I count observations for the concept Heart Rate"
        def responseData = get(request)

        then: "I get a count including observations from the restricted study"
        assert responseData.count == 7
    }

}
