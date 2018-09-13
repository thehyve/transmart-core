/* Copyright © 2017 The Hyve B.V. */
package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec
import org.springframework.http.HttpStatus
import org.transmartproject.core.multidimquery.crosstable.CrossTable
import org.transmartproject.core.multidimquery.ErrorResponse

import static base.ContentTypeFor.JSON
import static config.Config.*
import static tests.rest.constraints.*

class CrossTableSpec extends RESTSpec {

    /**
     *  Given: Studies EHR, SHARED_CONCEPTS_RESTRICTED and SHARED_CONCEPTS_A are loaded
     *  And: A patient set has been created
     *
     *  When: I specify a list of row and column constraints and the patient set in the subject constraint
     *  Then: For each cell the number of subjects is computed properly
     *
     *  When: I do not have an access to the specified patient set
     *  Then: Access is denied
     */
    @RequiresStudy([EHR_ID, SHARED_CONCEPTS_RESTRICTED_ID, SHARED_CONCEPTS_A_ID])
    def "get cross-table"() {
        given: 'Studies EHR, SHARED_CONCEPTS_RESTRICTED and SHARED_CONCEPTS_A are loaded'
        def patientSetRequest = [
                path      : PATH_PATIENT_SET,
                acceptType: JSON,
                query     : [name: 'crosstable_test_set'],
                body      : [
                        type: 'or',
                        args: [
                                [type: StudyNameConstraint, studyId: EHR_ID],
                                [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_RESTRICTED_ID]
                        ]
                ],
                user      : ADMIN_USER,
                statusCode: HttpStatus.CREATED.value()
        ]

        and: 'A patient set has been created'
        def patientSetResponse = post(patientSetRequest) as Map
        def patientSetId = patientSetResponse.id
        def restrictedConceptCode = 'SCSCP:DEM:AGE'
        def params = [
                rowConstraints   : [[type: TrueConstraint],
                                    [type: StudyNameConstraint, studyId: EHR_ID]],
                columnConstraints: [[type: ConceptConstraint, conceptCode: restrictedConceptCode],
                                    [type: TrueConstraint]],
                subjectConstraint: [type: PatientSetConstraint, patientSetId: patientSetId],
        ]
        def request = [
                path      : PATH_CROSSTABLE,
                acceptType: JSON,
                user      : ADMIN_USER,
                body      : params
        ]

        when: 'I specify a list of row and column constraints and the patient set in the subject constraint'
        def responseData = post(request) as CrossTable

        then: 'For each cell the number of subjects is computed properly'
        assert responseData.rows.size() == 2
        assert responseData.rows[0] == [2L, 5L]
        assert responseData.rows[1] == [0L, 3L]

        when: 'I do not have an access to the specified patient set'
        request.user = DEFAULT_USER
        request.statusCode = HttpStatus.FORBIDDEN.value()
        def errorResponse = post(request) as ErrorResponse

        then: 'Access is denied'
        errorResponse.httpStatus == HttpStatus.FORBIDDEN.value()
    }

    @RequiresStudy([EHR_ID, SHARED_CONCEPTS_RESTRICTED_ID, SHARED_CONCEPTS_A_ID])
    def "access denied for cross table for restricted constraints"() {
        given: 'I do not have an access to the restricted concept path constraint'
        def restrictedConceptCode = 'SCSCP:DEM:AGE'
        def params = [
                rowConstraints   : [[type: TrueConstraint],
                                    [type: StudyNameConstraint, studyId: EHR_ID]],
                columnConstraints: [[type: ConceptConstraint, conceptCode: restrictedConceptCode],
                                    [type: TrueConstraint]],
                subjectConstraint: [type: TrueConstraint],
        ]
        def request = [
                path      : PATH_CROSSTABLE,
                acceptType: JSON,
                user      : DEFAULT_USER,
                body      : params,
                statusCode: HttpStatus.FORBIDDEN.value()
        ]

        when: 'I request a cross table with a reference to a restricted concept in one of the constraints'
        def errorResponse = post(request) as ErrorResponse

        then: 'Access is denied'
        errorResponse.httpStatus == HttpStatus.FORBIDDEN.value()
    }

}
