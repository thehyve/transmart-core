package tests.rest.v2

import base.RESTSpec
import spock.lang.Requires

import static config.Config.*
import static tests.rest.v2.Operator.AND
import static tests.rest.v2.Operator.GREATER_THAN
import static tests.rest.v2.ValueType.NUMERIC
import static tests.rest.v2.constraints.*

class PatientsSetSpec extends RESTSpec {

    /**
     *  given: "Study EHR is loaded"
     *  when: "I make a patientset with age greater then greater then 30"
     *  then: "I get a patientset with 2 patients"
     */
    @Requires({EHR_LOADED})
    def "create patientset"(){
        given: "study EHR is loaded"

        when: "I make a patientset with age greater then greater then 30"
        def constraintMap = [
                type: Combination,
                operator: AND,
                args: [
                        [type: ConceptConstraint, path:"\\Public Studies\\EHR\\Demography\\Age\\"],
                        [type: ValueConstraint, valueType: NUMERIC, operator: GREATER_THAN, value:30]
                ]
        ]
        def responseData = post(PATH_PATIENT_SET, contentTypeForJSON, [name: 'test_set'], toJSON(constraintMap))

        then: "I get a patientset with 2 patients"
        assert  responseData.id != null
        assert get(PATH_PATIENTS, contentTypeForJSON, toQuery([type: PatientSetConstraint, patientSetId: responseData.id])).patients.size() == 2
    }

    /**
     *  given: "Studies with shared concepts is loaded"
     *  when: "I make a patient set with a shared concept"
     *  then: "the set has patients from several studies"
     */
    @Requires({SHARED_CONCEPTS_LOADED})
    def "create patient set shared concepts"(){
        given: "Studies with shared concepts is loaded"

        when: "I make a patient set with a shared concept"
        def constraintMap = [type: ConceptConstraint, path:"\\Vital Signs\\Heart Rate\\"]
        def responseData = post(PATH_PATIENT_SET, contentTypeForJSON, [name: 'test_set'], toJSON(constraintMap))

        then: "the set has patients from several studies"
        assert  responseData.id != null
        assert get(PATH_PATIENTS, contentTypeForJSON, toQuery([type: PatientSetConstraint, patientSetId: responseData.id])).patients.size() == 4
    }

    /**
     *  given: "Studies with shared concepts is loaded and I have access to some"
     *  when: "I make a patient set with a shared concept"
     *  then: "the set has patients from the studies I have access to"
     */
    @Requires({SHARED_CONCEPTS_LOADED && SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "create patient set shared concepts restricted"(){
        given: "Studies with shared concepts is loaded and I have acces to some"

        when: "I make a patient set with a shared concept"
        def constraintMap = [type: ConceptConstraint, path:"\\Vital Signs\\Heart Rate\\"]
        def responseData = post(PATH_PATIENT_SET, contentTypeForJSON, [name: 'test_set'], toJSON(constraintMap))

        then: "the set has patients from the studies I have access to"
        assert  responseData.id != null
        assert get(PATH_PATIENTS, contentTypeForJSON, toQuery([type: PatientSetConstraint, patientSetId: responseData.id])).patients.size() == 4
    }

    /**
     *  given: "Studies with shared concepts is loaded and I have access to all"
     *  when: "I make a patient set with a shared concept"
     *  then: "the set has patients from all studies with the shared concept"
     */
    @Requires({SHARED_CONCEPTS_LOADED && SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "create patient set shared concepts unrestricted"(){
        given: "Studies with shared concepts is loaded and I have access to all"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)

        when: "I make a patient set with a shared concept"
        def constraintMap = [type: ConceptConstraint, path:"\\Vital Signs\\Heart Rate\\"]
        def responseData = post(PATH_PATIENT_SET, contentTypeForJSON, [name: 'test_set'], toJSON(constraintMap))

        then: "the set has patients from all studies with the shared concept"
        assert  responseData.id != null
        assert get(PATH_PATIENTS, contentTypeForJSON, toQuery([type: PatientSetConstraint, patientSetId: responseData.id])).patients.size() == 6
    }

    /**
     *  given: "Studies with shared concepts is loaded and I have access to some"
     *  when: "When I use a patient set that contains patients that I do not have access to"
     *  then: "I get a access error"
     */
    @Requires({SHARED_CONCEPTS_LOADED && SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "using patient by user without access"(){
        given: "Studies with shared concepts is loaded and I have access to some"

        when: "When I use a patient set that contains patients that I do not have access to"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)
        def constraintMap = [type: ConceptConstraint, path:"\\Vital Signs\\Heart Rate\\"]
        def setID = post(PATH_PATIENT_SET, contentTypeForJSON, [name: 'test_set'], toJSON(constraintMap))
        setUser(DEFAULT_USERNAME, DEFAULT_PASSWORD)
        def responseData =  get(PATH_PATIENTS, contentTypeForJSON, toQuery([type: PatientSetConstraint, patientSetId: setID.id]))

        then: "I get a access error"
        assert responseData.httpStatus == 403
        assert responseData.type == 'AccessDeniedException'
        assert responseData.message == "Access denied to patient set: ${setID.id}"
    }
}
