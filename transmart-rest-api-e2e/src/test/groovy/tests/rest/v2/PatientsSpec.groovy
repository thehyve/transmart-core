/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v2

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.contentTypeForJSON
import static config.Config.*
import static org.hamcrest.Matchers.*
import static spock.util.matcher.HamcrestSupport.that
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.Operator.GREATER_THAN
import static tests.rest.v2.ValueType.NUMERIC
import static tests.rest.v2.constraints.*

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
        def request = [
                path      : PATH_PATIENTS,
                acceptType: contentTypeForJSON,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
                        args    : [
                                [type: ConceptConstraint, path: "\\Public Studies\\CLINICAL_TRIAL\\Vital Signs\\Heart Rate\\"],
                                [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value: 80]
                        ]
                ])
        ]

        when: "I get all patients from that study with a heart rate above 80"
        def responseData = get(request)

        then: "2 patients are returned"
        assert responseData.patients.size() == 2
        that responseData.patients, everyItem(hasKey('id'))
    }

    /**
     *  given: "study CLINICAL_TRIAL is loaded"
     *  when: "I get all patients from that study with a heart rate above 80"
     *  then: "2 patients are returned"
     */
    @RequiresStudy(CLINICAL_TRIAL_ID)
    def "get patients based on observations using POST method"() {
        given: "study CLINICAL_TRIAL is loaded"
        def request = [
                path      : PATH_PATIENTS,
                acceptType: contentTypeForJSON,
                body      : [
                        constraint: [
                                type    : Combination,
                                operator: AND,
                                args    : [
                                        [type: ConceptConstraint, path: "\\Public Studies\\CLINICAL_TRIAL\\Vital Signs\\Heart Rate\\"],
                                        [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value: 80]
                                ]
                        ]
                ]
        ]

        when: "I get all patients from that study with a heart rate above 80"
        def responseData = post(request)

        then: "2 patients are returned"
        assert responseData.patients.size() == 2
        that responseData.patients, everyItem(hasKey('id'))
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
                acceptType: contentTypeForJSON,
                query     : toQuery([
                        type    : Combination,
                        operator: AND,
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
                ])
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
                acceptType: contentTypeForJSON,
                query     : toQuery([type: StudyNameConstraint, studyId: SHARED_CONCEPTS_RESTRICTED_ID]),
                statusCode: 403
        ]

        when: "I try to get the patients from that study"
        def responseData = get(request)

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
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)
        def request = [
                path      : PATH_PATIENTS,
                acceptType: contentTypeForJSON,
                query     : toQuery([type: StudyNameConstraint, studyId: SHARED_CONCEPTS_RESTRICTED_ID]),
                statusCode: 403
        ]

        when: "I try to get the patients from that study"
        def responseData = get(request)

        then: "I get all patients"
        assert responseData.patients.size() == 2
        that responseData.patients, hasItems(
                hasEntry('id', -69),
                hasEntry('id', -59))
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "I try to get the patients from that study by id in path"
     *  then: "I get an not found error"
     */
    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "restricted get patient by id"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
        def request = [
                path      : PATH_PATIENTS + "/-69",
                acceptType: contentTypeForJSON,
                statusCode: 404
        ]

        when: "I try to get the patients from that study by id in path"
        def responseData = get(request)

        then: "I get an not found error"
        assert responseData.httpStatus == 404
        assert responseData.type == 'NoSuchResourceException'
        assert responseData.message == "Patient not found with id -69."
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
     *  when: "I try to get the patients from that study by id in path"
     *  then: "I get the patient"
     */
    @RequiresStudy(SHARED_CONCEPTS_RESTRICTED_ID)
    def "get patient by id"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)

        when: "I try to get the patients from that study by id in path"
        def responseData = get([
                path      : PATH_PATIENTS + "/-69",
                acceptType: contentTypeForJSON
        ])

        then: "I get the patient"
        assert responseData.id == -69
    }
}
