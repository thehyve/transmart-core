/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec
import base.RestHelper
import org.transmartproject.core.multidimquery.ErrorResponse

import static base.ContentTypeFor.JSON
import static config.Config.*
import static org.hamcrest.Matchers.*
import static org.springframework.http.HttpMethod.GET
import static org.springframework.http.HttpMethod.POST
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.Operator.GREATER_THAN
import static tests.rest.ValueType.NUMERIC
import static tests.rest.constraints.*

/**
 *  TMPREQ-10
 *      The REST API should support querying patients based on observations:
 *          certain constraints are valid for any or for all observations for the patient. E.g, all observations of high blood pressure occur after supply of drug X.
 */
class PatientsSpec extends RESTSpec {

    /**
     *  given: "study CLINICAL_TRIAL is loaded"
     *  when: "I get all patients from that study with a heart rate above 80"
     *  then: "2 patients are returned"
     */
    @RequiresStudy(CLINICAL_TRIAL_ID)
    def "get patients based on observations"() {
        given: "study CLINICAL_TRIAL is loaded"
        def params = [
                constraint: [
                        type    : 'and',
                        args    : [
                                [type: ConceptConstraint, path: "\\Public Studies\\CLINICAL_TRIAL\\Vital Signs\\Heart Rate\\"],
                                [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value: 80]
                        ]
                ]
        ]
        def request = [
                path      : PATH_PATIENTS,
                acceptType: JSON,
        ]

        when: "I get all patients from that study with a heart rate above 80"
        def responseData = getOrPostRequest(method, request, params)

        then: "2 patients are returned"
        assert responseData.patients.size() == 2
        that responseData.patients, everyItem(hasKey('id'))

        where:
        method  | _
        POST    | _
        GET     | _
    }

    /**
     *  given: "study CLINICAL_TRIAL is loaded"
     *  when: "I get all patients from that study that had a heart rate above 60 after 7 days (after trial visit 2)"
     *  then: "2 patients are returned"
     */
    @RequiresStudy(CLINICAL_TRIAL_ID)
    def "get patients by trial visit observation value"() {
        given: "study CLINICAL_TRIAL is loaded"
        def request = [
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : [constraint: [
                        type    : 'and',
                        args    : [
                                [type: ConceptConstraint, path: "\\Public Studies\\CLINICAL_TRIAL\\Vital Signs\\Heart Rate\\"],
                                [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value: 60],
                                [type    : FieldConstraint,
                                 field   : [dimension: 'trial visit',
                                            fieldName: 'relTime',
                                            type     : NUMERIC],
                                 operator: GREATER_THAN,
                                 value   : 7]
                        ]
                ]]
        ]

        when: "I get all patients from that study that had a heart rate above 60 after 7 days (after trial visit 2)"
        def responseData = get(request)

        then: "2 patients are returned"
        assert responseData.patients.size() == 2
        that responseData.patients, everyItem(hasKey('id'))
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "I try to get the patients from that study"
     *  then: "I get an access error"
     */
    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "get pratients restricted"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
        def request = [
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : [constraint: [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_RESTRICTED_ID]],
                statusCode: 403
        ]

        when: "I try to get the patients from that study"
        def responseData = RestHelper.toObject(get(request), ErrorResponse)

        then: "I get an access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
        assert responseData.message == "Access denied to study or study does not exist: ${SHARED_CONCEPTS_RESTRICTED_ID}"
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
     *  when: "I try to get the patients from that study"
     *  then: "I get all patients"
     */
    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "get pratients unrestricted"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
        def request = [
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : [constraint: [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_RESTRICTED_ID]],
                user      : UNRESTRICTED_USER
        ]

        when: "I try to get the patients from that study"
        def responseData = get(request)

        then: "I get all patients"
        assert responseData.patients.size() == 2
        responseData.patients.collect { it.subjectIds['SUBJ_ID'] } as Set == ['SCSC:69', 'SCSC:59'] as Set
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "I try to get the patients from that study by id in path"
     *  then: "I get an not found error"
     */
    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "restricted get patient by id"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
        def patientsRequest = [
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : [constraint: [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_RESTRICTED_ID]],
                user      : UNRESTRICTED_USER
        ]

        def patientsResponseData = get(patientsRequest)
        assert patientsResponseData.patients
        def patient = patientsResponseData.patients[0]
        assert patient.id

        def request = [
                path      : PATH_PATIENTS + '/' + patient.id,
                acceptType: JSON,
                statusCode: 404
        ]

        when: "I try to get the patients from that study by id in path"
        def responseData = RestHelper.toObject(get(request), ErrorResponse)

        then: "I get an not found error"
        assert responseData.httpStatus == 404
        assert responseData.type == 'NoSuchResourceException'
        assert responseData.message == "Patient not found with id ${patient.id}."
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
     *  when: "I try to get the patients from that study by id in path"
     *  then: "I get the patient"
     */
    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "get patient by id"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
        def patientsRequest = [
                path      : PATH_PATIENTS,
                acceptType: JSON,
                query     : [constraint: [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_RESTRICTED_ID]],
                user      : UNRESTRICTED_USER
        ]

        def patientsResponseData = get(patientsRequest)
        assert patientsResponseData.patients
        def patient = patientsResponseData.patients[0]
        assert patient.id

        when: "I try to get the patients from that study by id in path"
        def responseData = get([
                path      : PATH_PATIENTS + '/' + patient.id,
                acceptType: JSON,
                user      : UNRESTRICTED_USER
        ])

        then: "I get the patient"
        responseData.subjectIds == patient.subjectIds
    }
}
