/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.EHR_ID
import static config.Config.PATH_OBSERVATIONS
import static config.Config.PATH_TABLE
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
                constraint: toJSON([
                        type: ConceptConstraint,
                        path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                ]),
                type      : 'clinical'
        ]
        def request = [
                path      : PATH_OBSERVATIONS,
                acceptType: JSON,

        ]

        when: "for that study I get all observations for a heart rate"
        def responseData = getOrPostRequest(method, request, params)

        then: "9 observations are returned"
        assert responseData.cells.size() == 9

        where:
        method | _
        "POST" | _
        "GET"  | _
    }

    /**
     *
     * @return
     */
    @RequiresStudy(EHR_ID)
    def "get data table"() {
        given: "study EHR is loaded"
        def params = [
                constraint       : toJSON([
                        type: ConceptConstraint,
                        path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                ]),
                type             : 'clinical',
                rowDimensions    : ['patient', 'study'],
                columnDimensions : ['trial visit', 'concept'],
                columnSort       : toJSON([['trial visit', 'asc'], ['concept', 'desc']]),
                rowSort          : toJSON(['patient': 'desc']),
                limit            : 5,
                offset           : 3
        ]
        def request = [
                path      : PATH_TABLE,
                acceptType: JSON,

        ]

        when: "for that study I get all observations for a heart rate in table format"
        def responseData = getOrPostRequest(method, request, params)

        then: "table is properly formatted"
        assert responseData.column_dimensions.size() == 2
        assert responseData.column_headers.size() == 2
        assert responseData.row_dimensions.size() == 2
        assert responseData.rows.size() == 1
        assert responseData["row count"] == 1
        //assert responseData.offset == 3
        //assert responseData.sorting.find{ it.dimension == 'study'}.order == "desc"


        where:
        method | _
        "POST" | _
        "GET"  | _
    }
}
