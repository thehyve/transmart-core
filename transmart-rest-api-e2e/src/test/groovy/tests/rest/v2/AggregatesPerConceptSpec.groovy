/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.*
import static tests.rest.Operator.OR
import static tests.rest.constraints.Combination
import static tests.rest.constraints.ConceptConstraint

class AggregatesPerConceptSpec extends RESTSpec {

    /**
     *  given: "study EHR is loaded"
     *  when: "for that study I Aggregated the concept Heart Rate with type max"
     *  then: "the number 102.0 is returned"
     */
    @RequiresStudy(EHR_ID)
    def "aggregated timeseries maximum"() {
        given: "study EHR is loaded"
        def params = [
                constraint: toJSON(
                        [
                                type    : Combination,
                                operator: OR,
                                args    : [
                                        [
                                                type: ConceptConstraint,
                                                path: '\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\'
                                        ],

                                        [
                                                type: ConceptConstraint,
                                                path: '\\Public Studies\\CATEGORICAL_VALUES\\Demography\\Race\\'
                                        ]
                                ]
                        ]

                ),
        ]

        def request = [
                path      : PATH_AGGREGATES_PER_CONCEPT,
                acceptType: JSON
        ]

        when: "for that study I Aggregated the concept Heart Rate with type max"
        def responseData = getOrPostRequest(method, request, params)

        then:
        responseData.aggregatesPerConcept.size() == 2

        def hr = responseData.aggregatesPerConcept['EHR:VSIGN:HR']
        hr
        hr.size() == 1
        def numericalValueAggregates = hr.numericalValueAggregates
        numericalValueAggregates.min == 56
        numericalValueAggregates.max == 102
        numericalValueAggregates.count == 9
        Math.abs(numericalValueAggregates.avg - 74.78) < 0.01
        Math.abs(numericalValueAggregates.stdDev - 14.7) < 0.01

        def race = responseData.aggregatesPerConcept['CV:DEM:RACE']
        race
        race.size() == 1
        def categoricalValueAggregates = race.categoricalValueAggregates
        def valueCounts = categoricalValueAggregates.valueCounts
        valueCounts == [Caucasian: 2, Latino: 1]

        where:
        method | _
        "POST" | _
        "GET"  | _
    }

    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "test access rights respected"() {
        def conceptPath = '\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\'

        when: "I do not have access for that study. I Aggregated the concept Heart Rate."
        def responseData = get([
                path      : PATH_AGGREGATES_PER_CONCEPT,
                acceptType: JSON,
                query     : [
                        constraint: toJSON([type: ConceptConstraint, path: conceptPath])
                ],
                statusCode: 403
        ])

        then: "I get an access error"
        responseData.httpStatus == 403
        responseData.type == 'AccessDeniedException'
        responseData.message == "Access denied to concept path: ${conceptPath}"

        when: "I have access for that study. I Aggregated the concept Heart Rate."
        responseData = get([
                path      : PATH_AGGREGATES_PER_CONCEPT,
                acceptType: JSON,
                query     : [
                        constraint: toJSON([type: ConceptConstraint, path: conceptPath])
                ],
                user      : UNRESTRICTED_USER
        ])
        then: "I get a result"
        responseData.aggregatesPerConcept
    }

}
