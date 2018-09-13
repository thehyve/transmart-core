/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec
import base.RestHelper
import org.transmartproject.core.multidimquery.ErrorResponse

import static base.ContentTypeFor.JSON
import static config.Config.*
import static tests.rest.Operator.AND
import static tests.rest.Operator.GREATER_THAN
import static tests.rest.ValueType.NUMERIC
import static tests.rest.constraints.*

class PatientsSetSpec extends RESTSpec {

    /**
     *  given: "Study EHR is loaded"
     *  when: "I make a patientset with age greater then greater then 30"
     *  then: "I get a patientset with 2 patients"
     */
    @RequiresStudy(EHR_ID)
    def "create patientset"() {
        given: "study EHR is loaded"
        def request = [
                path      : PATH_PATIENT_SET,
                acceptType: JSON,
                query     : [name: 'test_set'],
                body      : [
                        type    : 'and',
                        args    : [
                                [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Demography\\Age\\"],
                                [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value: 30]
                        ]
                ],
                statusCode: 201
        ]

        when: "I make a patientset with age greater then greater then 30"
        def responseData = post(request)

        then: "I get a patientset with 2 patients"
        assert responseData.id != null
        assert get([
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : [constraint: [type: PatientSetConstraint, patientSetId: responseData.id]]
        ]).patients.size() == 2

    }

    /**
     *  given: "Studies with shared concepts is loaded"
     *  when: "I make a patient set with a shared concept"
     *  then: "the set has patients from several studies"
     */
    @RequiresStudy([SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "create patient set shared concepts"() {
        given: "Studies with shared concepts is loaded"
        def request = [
                path      : PATH_PATIENT_SET,
                acceptType: JSON,
                query     : [name: 'test_set'],
                body      : [type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"],
                statusCode: 201
        ]

        when: "I make a patient set with a shared concept"
        def responseData = post(request)

        then: "the set has patients from several studies"
        assert responseData.id != null
        assert get([
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : [constraint: [type: PatientSetConstraint, patientSetId: responseData.id]]
        ]).patients.size() == 4
    }

    /**
     *  given: "Studies with shared concepts is loaded and I have access to some"
     *  when: "I make a patient set with a shared concept"
     *  then: "the set has patients from the studies I have access to"
     */
    @RequiresStudy([SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "create patient set shared concepts restricted"() {
        given: "Studies with shared concepts is loaded and I have acces to some"
        def request = [
                path      : PATH_PATIENT_SET,
                acceptType: JSON,
                query     : [name: 'test_set'],
                body      : [type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"],
                statusCode: 201
        ]

        when: "I make a patient set with a shared concept"
        def responseData = post(request)

        then: "the set has patients from the studies I have access to"
        assert responseData.id != null
        assert get([
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : [constraint: [type: PatientSetConstraint, patientSetId: responseData.id]]
        ]).patients.size() == 4
    }

    /**
     *  given: "Studies with shared concepts is loaded and I have access to all"
     *  when: "I make a patient set with a shared concept"
     *  then: "the set has patients from all studies with the shared concept"
     */
    @RequiresStudy([SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "create patient set shared concepts unrestricted"() {
        given: "Studies with shared concepts is loaded and I have access to all"
        def request = [
                path      : PATH_PATIENT_SET,
                acceptType: JSON,
                query     : [name: 'test_set'],
                body      : [type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"],
                statusCode: 201,
                user      : UNRESTRICTED_USER
        ]

        when: "I make a patient set with a shared concept"
        def responseData = post(request)

        then: "the set has patients from all studies with the shared concept"
        assert responseData.id != null
        assert get([
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : [constraint: [type: PatientSetConstraint, patientSetId: responseData.id]],
                user      : UNRESTRICTED_USER
        ]).patients.size() == 6
    }

    /**
     *  given: "Studies with shared concepts is loaded and I have access to some"
     *  when: "When I use a patient set that contains patients that I do not have access to"
     *  then: "I get a access error"
     */
    @RequiresStudy([SHARED_CONCEPTS_A_ID, SHARED_CONCEPTS_B_ID, SHARED_CONCEPTS_RESTRICTED_ID])
    def "using patient by user without access"() {
        given: "Studies with shared concepts is loaded and I have access to some"
        def request = [
                path      : PATH_PATIENT_SET,
                acceptType: JSON,
                query     : [name: 'test_set'],
                body      : [type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"],
                statusCode: 201,
                user      : UNRESTRICTED_USER
        ]
        def response = post(request)
        int setID = response.id

        when: "When I use a patient set that contains patients that I do not have access to"
        def responseData = RestHelper.toObject(get([
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : [constraint: [type: PatientSetConstraint, patientSetId: setID]],
                statusCode: 403
        ]), ErrorResponse)

        then: "I get a access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
        assert responseData.message == "Access denied to patient set or patient set does not exist: ${setID}"
    }

    /**
     *  when: "I try to fetch all patientSets"
     *  then: "the list of all patientSets is returned"
     */

    @RequiresStudy(EHR_ID)
    def "get list of patientSets"() {
        given: "at least one patient_set exists"
        def createPatientSetRequest = [
                path      : PATH_PATIENT_SET,
                acceptType: JSON,
                query     : [name: 'test_set'],
                body      : [
                        type    : Combination,
                        operator: AND,
                        args    : [
                                [type: ConceptConstraint, path: "\\Public Studies\\EHR\\Demography\\Age\\"],
                                [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value: 30]
                        ]
                ],
                statusCode: 201
        ]
        def newSet = post(createPatientSetRequest)
        def request = [
                path      : PATH_PATIENT_SET,
                acceptType: JSON,
        ]

        when: "I try to fetch all patientSets"
        def responseData = get(request)

        then: "the list of all patientSets is returned"
        assert newSet in responseData.patientSets
        responseData.patientSets.each {
            it.keySet().containsAll(['description', 'errorMessage', 'id', 'setSize', 'status', 'username'])
        }
    }
}
