/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.JSON
import static config.Config.*
import static tests.rest.Operator.OR
import static tests.rest.constraints.*

class CrossTableSpec extends RESTSpec {

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
                body      : [
                        type    : Combination,
                        operator: OR,
                        args    : [
                                [type: StudyNameConstraint, studyId: EHR_ID],
                                [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_RESTRICTED_ID]
                        ]
                ],
                user      : ADMIN_USER,
                statusCode: 201
        ]
        def patientSetResponse = post(patientSetRequest) as Map
        def patientSetId = patientSetResponse.id
        def restrictedConceptPath = '\\Private Studies\\SHARED_CONCEPTS_STUDY_C_PRIV\\Demography\\Age\\'
        def params = [
                rowConstraints   : [[type: TrueConstraint],
                                    [type: StudyNameConstraint, studyId: EHR_ID]],
                columnConstraints: [[type: ConceptConstraint, path: restrictedConceptPath],
                                    [type: TrueConstraint]],
                subjectConstraint: [type: PatientSetConstraint, patientSetId: patientSetId],
        ]
        def request = [
                path: PATH_CROSSTABLE,
                acceptType: JSON,
                user: ADMIN_USER,
                body: params
        ]

        when: "I specify a list of row and column constraints and their intersections create table cells"
        def responseData = post(request) as Map

        then: "for each of cells the number of subjects is computed properly"
        assert responseData.rows.size() == 2
        assert responseData.rows[0] == [2, 5]
        assert responseData.rows[1] == [0, 3]

        when: "I do not have an access to the specified patient set"
        def request2 = request
        request2.user = DEFAULT_USER
        def responseData2 = post(request2) as Map

        then: "Returned counts for all cells equal zero"
        assert responseData2.rows.size() == 2
        assert responseData2.rows[0] == [0, 0]
        assert responseData2.rows[1] == [0, 0]

        when: "I do not have an access to the restricted concept path constraint"
        def patientSetRequest2 = [
                path      : PATH_PATIENT_SET,
                acceptType: JSON,
                query     : [name: 'crosstable_test_set'],
                body      : [
                        type    : Combination,
                        operator: OR,
                        args    : [
                                [type: StudyNameConstraint, studyId: EHR_ID],
                                [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_A_ID]
                        ]
                ],
                user      : ADMIN_USER,
                statusCode: 201
        ]
        patientSetRequest2.user = DEFAULT_USER
        def patientSetResponse2 = post(patientSetRequest2) as Map
        def patientSetId2 = patientSetResponse2.id
        def subjectConstraint2 = [type: PatientSetConstraint, patientSetId: patientSetId2]
        def request3 = request2
        request3.body.subjectConstraint = subjectConstraint2
        def responseData3 = post(request3) as Map

        then: "Returned count for the first cell equals zero"
        assert responseData3.rows.size() == 2
        assert responseData3.rows[0] == [0, 5]
        assert responseData3.rows[1] == [0, 3]
    }

}
