/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.*
import static tests.rest.QueryType.*
import static tests.rest.constraints.ConceptConstraint

/**
 *
 *  TMPREQ-10
 *  The REST API should support quering for the following aggregated values for numerical data:
 *      minimum
 *      maximum
 *      average
 */
class AggregatedSpec extends RESTSpec {

    /**
     *  given: "study EHR is loaded"
     *  when: "for that study I Aggregated the concept Heart Rate with type max"
     *  then: "the number 102.0 is returned"
     */
    @RequiresStudy(EHR_ID)
    def "aggregated timeseries maximum"() {

        given: "study EHR is loaded"
        def params = [
                constraint: toJSON([
                        type: ConceptConstraint,
                        path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                ]),
                type      : MAX
        ]
        def request = [
                path      : PATH_AGGREGATE,
                acceptType: JSON
        ]

        when: "for that study I Aggregated the concept Heart Rate with type max"
        def responseData = getOrPostRequest(method, request, params)

        then: "the number 102 is returned"
        assert responseData.max == 102.0

        where:
        method | _
        "POST" | _
        "GET"  | _
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "for that study I Aggregated the concept Heart Rate with type min"
     *  then: "the number 56.0 is returned"
     */
    @RequiresStudy(EHR_ID)
    def "aggregated timeseries minimum"() {
        given: "study EHR is loaded"
        def request = [
                path      : PATH_AGGREGATE,
                acceptType: JSON,
                query     : [
                        constraint: toJSON([
                                type: ConceptConstraint,
                                path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                        ]),
                        type      : MIN
                ]
        ]

        when: "for that study I Aggregated the concept Heart Rate with type min"
        def responseData = get(request)

        then: "the number 56.0 is returned"
        assert responseData.min == 56.0
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "for that study I Aggregated the concept Heart Rate with type average"
     *  then: "the number 74.77777777777777 is returned"
     */
    @RequiresStudy(EHR_ID)
    def "aggregated timeseries average"() {
        given: "study EHR is loaded"
        def request = [
                path      : PATH_AGGREGATE,
                acceptType: JSON,
                query     : [
                        constraint: toJSON([
                                type: ConceptConstraint,
                                path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                        ]),
                        type      : AVERAGE
                ]
        ]

        when: "for that study I Aggregated the concept Heart Rate with type average"
        def responseData = get(request)

        then: "the number 74.77777777777777 is returned"
        assert responseData.average == 74.77777777777777
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "for that study I Aggregated the concept Heart Rate without a type"
     *  then: "an error is returned"
     */
    @RequiresStudy(EHR_ID)
    def "aggregated missing type"() {
        given: "study EHR is loaded"
        def request = [
                path      : PATH_AGGREGATE,
                acceptType: JSON,
                query     : [
                        constraint: toJSON([
                                type: ConceptConstraint,
                                path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                        ])
                ],
                statusCode: 400
        ]

        when: "for that study I Aggregated the concept Heart Rate without a type"
        def responseData = get(request)

        then: "an error is returned"
        assert responseData.httpStatus == 400
        assert responseData.message == 'Type parameter is missing.'
        assert responseData.type == 'InvalidArgumentsException'
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "for that study I Aggregated the concept Heart Rate with type average"
     *  then: "I get an access error"
     */
    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "restricted aggregated average"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
        def conceptPath = '\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\'
        def request = [
                path      : PATH_AGGREGATE,
                acceptType: JSON,
                query     : [
                        constraint: toJSON([type: ConceptConstraint, path: conceptPath]),
                        type      : AVERAGE
                ],
                statusCode: 403
        ]

        when: "for that study I Aggregated the concept Heart Rate with type average"
        def responseData = get(request)

        then: "I get an access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
        assert responseData.message == "Access denied to concept path: ${conceptPath}"
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
     *  when: "for that study I Aggregated the concept Heart Rate with type average"
     *  then: "I get an average"
     */
    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "unrestricted aggregated average"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
        def conceptPath = '\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\'
        def request = [
                path      : PATH_AGGREGATE,
                acceptType: JSON,
                query     : [
                        constraint: toJSON([type: ConceptConstraint, path: conceptPath]),
                        type      : AVERAGE
                ],
                user      : UNRESTRICTED_USER
        ]

        when: "for that study I Aggregated the concept Heart Rate with type average"
        def responseData = get(request)

        then: "I get an average"
        assert responseData.average == 34.5
    }

}
