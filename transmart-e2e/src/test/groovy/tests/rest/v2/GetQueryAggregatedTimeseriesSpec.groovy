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
 *  aggregated timeseries/samples values:
 *      minimum
 *      maximum
 *      average
 */
class GetQueryAggregatedTimeseriesSpec extends RESTSpec{

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

        def responseData = get("query/aggregate", contentTypeForJSON, query)

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

        def responseData = get("query/aggregate", contentTypeForJSON, query)

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

        def responseData = get("query/aggregate", contentTypeForJSON, query)

        then: "the number 74.77777777777777 is returned"
        assert responseData.average == 74.77777777777777
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "for that study I Aggregated the concept Heart Rate without a type"
     *  then: "an error is returned"
     */
    @Requires({EHR_LOADED})
    def "aggregated timeseries missing type"(){
        given: "study EHR is loaded"

        when: "for that study I Aggregated the concept Heart Rate without a type"
        def query = [
                constraint: toJSON([
                        type: ConceptConstraint,
                        path:"\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                ])
        ]

        def responseData = get("query/aggregate", contentTypeForJSON, query)

        then: "an error is returned"
        assert responseData.httpStatus == 400
        assert responseData.message == 'Type parameter is missing.'
        assert responseData.type == 'InvalidArgumentsException'
    }
}
