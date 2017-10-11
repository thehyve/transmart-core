/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.*
import static tests.rest.constraints.ConceptConstraint

@RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
class CategoricalValueFrequenciesSpec extends RESTSpec {

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "I get frequency of categorical values for the concept 'Place of birth'"
     *  then: "I get a count excluding values from the restricted study"
     */
    def "restricted count"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
        def request = [
                path      : PATH_CATEGORICAL_VALUE_FREQ,
                acceptType: JSON,
                query     : toQuery([type: ConceptConstraint, path: "\\Demographics\\Place of birth\\"])
        ]

        when: "I count categorical values for the concept 'Place of birth'"
        def responseData = get(request)

        then: "I get a value frequencies excluding values from the restricted study"
        responseData.Place1 == 1
        responseData.Place2 == 3
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
     *  when: "I get frequency of categorical values for the concept 'Place of birth'"
     *  then: "I get a count including values from the restricted study"
     */
    def "unrestricted count"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
        def params = [
                constraint: toJSON([type: ConceptConstraint, path: "\\Demographics\\Place of birth\\"])
        ]
        def request = [
                path      : PATH_CATEGORICAL_VALUE_FREQ,
                acceptType: JSON,
                user      : UNRESTRICTED_USER
        ]

        when: "I count categorical values for the concept 'Place of birth'"
        def responseData = getOrPostRequest(method, request, params)

        then: "I get a value frequencies excluding values from the restricted study"
        responseData.Place1 == 2
        responseData.Place2 == 4

        where:
        method | _
        "POST" | _
        "GET"  | _
    }
}
