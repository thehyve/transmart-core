/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec
import org.springframework.http.HttpStatus
import representations.AggregatesPerConcept
import representations.ErrorResponse

import static base.ContentTypeFor.JSON
import static config.Config.*

class AggregatesPerConceptSpec extends RESTSpec {

    /**
     *  Given: Studies EHR and CATEGORICAL_VALUES are loaded
     *  When: I request aggregates for the Heart Rate and Race concepts
     *  Then: The expected numerical and categorical aggregates are returned
     */
    @RequiresStudy([EHR_ID, CATEGORICAL_VALUES_ID])
    def "numerical and categorical aggregates"() {
        given: 'Studies EHR and CATEGORICAL_VALUES are loaded'
        def params = [
                constraint: [
                        type    : 'or',
                        args    : [
                                [type: 'concept', conceptCode: 'EHR:VSIGN:HR'],
                                [type: 'concept', conceptCode: 'CV:DEM:RACE']
                        ]
                ]
        ]

        def request = [
                path      : PATH_AGGREGATES_PER_CONCEPT,
                acceptType: JSON,
                body: params
        ]

        when: 'I request aggregates for the Heart Rate and Race concepts'
        def responseData = post(request) as AggregatesPerConcept

        then: 'The expected numerical and categorical aggregates are returned'
        responseData.aggregatesPerConcept.size() == 2

        def hr = responseData.aggregatesPerConcept['EHR:VSIGN:HR']
        hr
        def numericalValueAggregates = hr.numericalValueAggregates
        numericalValueAggregates.min == 56
        numericalValueAggregates.max == 102
        numericalValueAggregates.count == 9
        Math.abs(numericalValueAggregates.avg - 74.78) < 0.01
        Math.abs(numericalValueAggregates.stdDev - 14.7) < 0.01

        def race = responseData.aggregatesPerConcept['CV:DEM:RACE']
        race
        def categoricalValueAggregates = race.categoricalValueAggregates
        def valueCounts = categoricalValueAggregates.valueCounts
        valueCounts == [Caucasian: 2, Latino: 1]
    }

    /**
     *  Given: Study Survey 1 is loaded
     *  When: I request aggregates for the favouritebook concept (of type Raw text)
     *  Then: No values for that concept are returned
     */
    @RequiresStudy(SURVEY1_ID)
    def "aggregates for raw text concept does not contain values"() {
        given: 'Study Survey 1 is loaded'
        def params = [
                constraint: [type: 'concept', conceptCode: 'favouritebook']
        ]

        def request = [
                path      : PATH_AGGREGATES_PER_CONCEPT,
                acceptType: JSON,
                body: params
        ]

        when: 'I request aggregates for the favouritebook concept (of type Raw text)'
        def response = post(request) as AggregatesPerConcept

        then: 'No values for that concept are returned'
        response.aggregatesPerConcept.size() == 0
    }

    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "access policy respected for aggregates call"() {
        given: 'I do not have access to the restricted access study'
        def conceptCode = 'SCSCP:DEM:AGE'

        when: 'I request aggregates for a concept only associated with that study'
        def errorResponse = post([
                path      : PATH_AGGREGATES_PER_CONCEPT,
                acceptType: JSON,
                body     : [
                        constraint: [type: 'concept', conceptCode: conceptCode]
                ],
                statusCode: HttpStatus.FORBIDDEN.value()
        ]) as ErrorResponse

        then: 'Access is denied'
        errorResponse.httpStatus == HttpStatus.FORBIDDEN.value()
        errorResponse.type == 'AccessDeniedException'
        errorResponse.message == "Access denied to concept code: ${conceptCode}"

        when: 'I do have access to the restricted access study'

        and: 'I request aggregates for a concept only associated with that study'
        def response = post([
                path      : PATH_AGGREGATES_PER_CONCEPT,
                acceptType: JSON,
                body     : [
                        constraint: [type: 'concept', conceptCode: conceptCode]
                ],
                user      : UNRESTRICTED_USER
        ]) as AggregatesPerConcept

        then: 'I receive aggregates'
        response.aggregatesPerConcept
        response.aggregatesPerConcept.containsKey(conceptCode)
        response.aggregatesPerConcept[conceptCode].numericalValueAggregates
    }

}
