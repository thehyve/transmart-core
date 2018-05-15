/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.ADMIN_USER
import static config.Config.DEFAULT_USER
import static config.Config.EHR_ID
import static config.Config.PATH_CROSSTABLE
import static config.Config.PATH_OBSERVATIONS
import static config.Config.PATH_PATIENT_SET
import static config.Config.PATH_TABLE
import static config.Config.SHARED_CONCEPTS_A_ID
import static config.Config.SHARED_CONCEPTS_RESTRICTED_ID
import static tests.rest.Operator.GREATER_THAN
import static tests.rest.Operator.OR
import static tests.rest.ValueType.NUMERIC
import static tests.rest.constraints.Combination
import static tests.rest.constraints.ConceptConstraint
import static tests.rest.constraints.PatientSetConstraint
import static tests.rest.constraints.StudyNameConstraint
import static tests.rest.constraints.TrueConstraint
import static tests.rest.constraints.ValueConstraint

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
     *  given: "study EHR is loaded"
     *  when: "for that study I get all observations for a heart rate in table format"
     *  then: "table is properly formatted"
     */
    @RequiresStudy(EHR_ID)
    def "get data table"() {
        given: "study EHR is loaded"
        def limit = 10
        def params = [
                constraint       : toJSON([
                        type: ConceptConstraint,
                        path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                ]),
                type             : 'clinical',
                rowDimensions    : toJSON(['patient', 'study']),
                columnDimensions : toJSON(['trial visit', 'concept']),
                columnSort       : toJSON([[dimension: 'trial visit', sortOrder: 'asc'], [dimension: 'concept', sortOrder: 'desc']]),
                rowSort          : toJSON([[dimension: 'patient', sortOrder: 'desc']]),
                limit            : limit,
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
        assert responseData.rows.size() == 3
        assert responseData["row count"] == 3
        assert responseData.offset == 0
        assert responseData.sort.find{ it.dimension == 'study'}.sortOrder == "asc"
        assert responseData.sort.find{ it.dimension == 'patient'}.sortOrder == "desc"
        assert responseData.sort.find{ it.dimension == 'trial visit'}.sortOrder == "asc"
        assert responseData.sort.find{ it.dimension == 'concept'}.sortOrder == "desc"

        when: "I specify an offset"
        def offset = 2
        limit = 2
        params['offset'] = offset
        params['limit'] = limit
        def responseData2 = getOrPostRequest(method, request, params)

        then: "number of results has decreased"
        assert responseData2.offset == offset
        assert responseData2["row count"] == responseData["row count"]
        assert responseData2.rows.size() == limit
        assert responseData2.rows == responseData.rows.takeRight(2)

        where:
        method | _
        "POST" | _
        "GET"  | _
    }

    /**
     *  given: "study EHR, SHARED_CONCEPTS_RESTRICTED and SHARED_CONCEPTS_A are loaded"
     *  when: "I specify a list of row and column constraints and their intersections create table cells"
     *  then: "for each of cells the number of subjects is computed properly"
     */
    @RequiresStudy([EHR_ID, SHARED_CONCEPTS_RESTRICTED_ID, SHARED_CONCEPTS_A_ID])
    def "get cross-table"() {
        def patientSetRequest = [
                path      : PATH_PATIENT_SET,
                acceptType: JSON,
                query     : [name: 'crosstable_test_set'],
                body      : toJSON([
                        type    : Combination,
                        operator: OR,
                        args    : [
                                [type: StudyNameConstraint, studyId: EHR_ID],
                                [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_RESTRICTED_ID]
                        ]
                ]),
                user      : ADMIN_USER,
                statusCode: 201
        ]
        def patientSetResponse = post(patientSetRequest)
        def patientSetId = patientSetResponse.id
        def restrictedConceptPath = '\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\'
        def params = [
                rowConstraints   : [toJSON([type: TrueConstraint]),
                                    toJSON([type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value: 80])],
                columnConstraints: [toJSON([type: ConceptConstraint, path: restrictedConceptPath]),
                                    toJSON([type: TrueConstraint])],
                subjectConstraint: toJSON(type: PatientSetConstraint, patientSetId: patientSetId),
        ]
        def request = [
                path      : PATH_CROSSTABLE,
                acceptType: JSON,
                user      : ADMIN_USER
        ]
        when: "I specify a list of row and column constraints and their intersections create table cells"
        def responseData = getOrPostRequest(method, request, params)

        then: "for each of cells the number of subjects is computed properly"
        assert responseData.rows.size() == 2
        assert responseData.rows[0] == [2, 5]
        assert responseData.rows[1] == [0, 3]

        when: "I do not have an access to the specified patient set"
        def request2 = request
        request2.user = DEFAULT_USER
        def responseData2 = getOrPostRequest(method, request2, params)

        then: "Returned counts for all cells equal zero"
        assert responseData2.rows.size() == 2
        assert responseData2.rows[0] == [0, 0]
        assert responseData2.rows[1] == [0, 0]

        when: "I do not have an access to the restricted concept path constraint"
        def patientSetRequest2 = [
                path      : PATH_PATIENT_SET,
                acceptType: JSON,
                query     : [name: 'crosstable_test_set'],
                body      : toJSON([
                        type    : Combination,
                        operator: OR,
                        args    : [
                                [type: StudyNameConstraint, studyId: EHR_ID],
                                [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_A_ID]
                        ]
                ]),
                user      : ADMIN_USER,
                statusCode: 201
        ]
        patientSetRequest2.user = DEFAULT_USER
        def patientSetResponse2 = post(patientSetRequest2)
        def patientSetId2 = patientSetResponse2.id
        def subjectConstraint2 = toJSON(type: PatientSetConstraint, patientSetId: patientSetId2)
        def params2 = params
        params2.subjectConstraint = subjectConstraint2
        def responseData3 = getOrPostRequest(method, request2, params2)

        then: "Returned count for the first cell equals zero"
        assert responseData3.rows.size() == 2
        assert responseData3.rows[0] == [0, 5]
        assert responseData3.rows[1] == [0, 3]

        where:
        method | _
        "POST" | _
        "GET"  | _
    }
}
