package tests.rest.v2

import base.RESTSpec
import spock.lang.Requires

import static tests.rest.v2.QueryType.AVERAGE
import static tests.rest.v2.QueryType.MAX
import static tests.rest.v2.QueryType.MIN
import static tests.rest.v2.constraints.ConceptConstraint
import static config.Config.*

/**
 *
 *  TMPREQ-10
 *  The REST API should support quering for the following aggregated values for numerical data:
 *      minimum
 *      maximum
 *      average
 */
class AggregatedSpec extends RESTSpec{

    /**
     *  given: "study EHR is loaded"
     *  when: "for that study I Aggregated the concept Heart Rate with type max"
     *  then: "the number 102.0 is returned"
     */
    @Requires({EHR_LOADED})
    def "aggregated timeseries maximum"(){
        given: "study EHR is loaded"

        when: "for that study I Aggregated the concept Heart Rate with type max"
        def query = [
                constraint: toJSON([
                        type: ConceptConstraint,
                        path:"\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                ]),
                type: MAX
        ]

        def responseData = get(PATH_AGGREGATE, contentTypeForJSON, query)

        then: "the number 102 is returned"
        assert responseData.max == 102.0
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "for that study I Aggregated the concept Heart Rate with type min"
     *  then: "the number 56.0 is returned"
     */
    @Requires({EHR_LOADED})
    def "aggregated timeseries minimum"(){
        given: "study EHR is loaded"

        when: "for that study I Aggregated the concept Heart Rate with type min"
        def query = [
                constraint: toJSON([
                        type: ConceptConstraint,
                        path:"\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                ]),
                type: MIN
        ]

        def responseData = get(PATH_AGGREGATE, contentTypeForJSON, query)

        then: "the number 56.0 is returned"
        assert responseData.min == 56.0
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "for that study I Aggregated the concept Heart Rate with type average"
     *  then: "the number 74.77777777777777 is returned"
     */
    @Requires({EHR_LOADED})
    def "aggregated timeseries average"(){
        given: "study EHR is loaded"

        when: "for that study I Aggregated the concept Heart Rate with type average"
        def query = [
                constraint: toJSON([
                        type: ConceptConstraint,
                        path:"\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                ]),
                type: AVERAGE
        ]

        def responseData = get(PATH_AGGREGATE, contentTypeForJSON, query)

        then: "the number 74.77777777777777 is returned"
        assert responseData.average == 74.77777777777777
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "for that study I Aggregated the concept Heart Rate without a type"
     *  then: "an error is returned"
     */
    @Requires({EHR_LOADED})
    def "aggregated missing type"(){
        given: "study EHR is loaded"

        when: "for that study I Aggregated the concept Heart Rate without a type"
        def query = [
                constraint: toJSON([
                        type: ConceptConstraint,
                        path:"\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                ])
        ]

        def responseData = get(PATH_AGGREGATE, contentTypeForJSON, query)

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
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "restricted aggregated average"(){
        given: "study EHR is loaded"
        def conceptPath = '\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\'

        when: "for that study I Aggregated the concept Heart Rate with type average"
        def query = [
                constraint: toJSON([type: ConceptConstraint, path: conceptPath]),
                type: AVERAGE
        ]

        def responseData = get(PATH_AGGREGATE, contentTypeForJSON, query)

        then: "I get an access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
        assert responseData.message == "Access denied to concept path: ${conceptPath}"
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "for that study I Aggregated the concept Heart Rate with type average"
     *  then: "I get an access error"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "unrestricted aggregated average"(){
        given: "study EHR is loaded"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)
        def conceptPath = '\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\'

        when: "for that study I Aggregated the concept Heart Rate with type average"
        def query = [
                constraint: toJSON([type: ConceptConstraint, path: conceptPath]),
                type: AVERAGE
        ]

        def responseData = get(PATH_AGGREGATE, contentTypeForJSON, query)

        then: "the number 34.5 is returned"
        assert responseData.average == 34.5
    }

}
