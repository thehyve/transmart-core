package tests.rest.v2

import base.RESTSpec
import spock.lang.Requires

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
class PatientsSpec extends RESTSpec{

    /**
     *  given: "study CLINICAL_TRIAL is loaded"
     *  when: "I get all patients from that study with a heart rate above 80"
     *  then: "2 patients are returned"
     */
    @Requires({CLINICAL_TRIAL_LOADED})
    def "get patients based on observations"(){
        given: "study CLINICAL_TRIAL is loaded"

        when: "I get all patients from that study with a heart rate above 80"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: ConceptConstraint, path:"\\Public Studies\\CLINICAL_TRIAL\\Vital Signs\\Heart Rate\\"],
                        [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value:80]
                ]
        ]
        def responseData = get(PATH_PATIENTS, contentTypeForJSON, toQuery(constraintMap))

        then: "2 patients are returned"
        responseData.patients.size() == 2
        that responseData.patients, everyItem(hasKey('id'))
    }

    /**
     *  given: "study CLINICAL_TRIAL is loaded"
     *  when: "I get all patients from that study that had a heart rate above 60 after 7 days (after trial visit 2)"
     *  then: "2 patients are returned"
     */
    @Requires({CLINICAL_TRIAL_LOADED})
    def "get patients by trial visit observation value"(){
        given: "study CLINICAL_TRIAL is loaded"

        when: "I get all patients from that study that had a heart rate above 60 after 7 days (after trial visit 2)"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: ConceptConstraint, path:"\\Public Studies\\CLINICAL_TRIAL\\Vital Signs\\Heart Rate\\"],
                        [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value:60],
                        [type: FieldConstraint,
                         field: [dimension: 'TrialVisitDimension',
                                 fieldName: 'relTime',
                                 type: NUMERIC ],
                         operator: GREATER_THAN,
                         value:7]
                ]
        ]
        def responseData = get(PATH_PATIENTS, contentTypeForJSON, toQuery(constraintMap))

        then: "2 patients are returned"
        responseData.patients.size() == 2
        that responseData.patients, everyItem(hasKey('id'))
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "I try to get the patients from that study"
     *  then: "I get an access error"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "get pratients restricted"(){
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"

        when: "I try to get the patients from that study"
        def constraintMap = [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_RESTRICTED_ID]
        def responseData = get(PATH_PATIENTS, contentTypeForJSON, toQuery(constraintMap))

        then: "I get an access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
        assert responseData.message == "Access denied to study: ${SHARED_CONCEPTS_RESTRICTED_ID}"
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
     *  when: "I try to get the patients from that study"
     *  then: "I get all patients"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "get pratients unrestricted"(){
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)

        when: "I try to get the patients from that study"
        def constraintMap = [type: StudyNameConstraint, studyId: SHARED_CONCEPTS_RESTRICTED_ID]
        def responseData = get(PATH_PATIENTS, contentTypeForJSON, toQuery(constraintMap))

        then: "I get all patients"
        responseData.patients.size() == 2
        that responseData.patients, hasItems(
                hasEntry('id', -69),
                hasEntry('id', -59))
    }

}
