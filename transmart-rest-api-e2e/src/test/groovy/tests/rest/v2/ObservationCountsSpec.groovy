package tests.rest.v2

import base.RESTSpec
import base.RestCall
import spock.lang.Requires

import static config.Config.PATH_COUNTS
import static config.Config.SHARED_CONCEPTS_RESTRICTED_LOADED
import static config.Config.UNRESTRICTED_PASSWORD
import static config.Config.UNRESTRICTED_USERNAME
import static tests.rest.v2.constraints.ConceptConstraint

class ObservationCountsSpec extends RESTSpec{

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
     *  when: "I count observations for the concept Heart Rate"
     *  then: "I get a count including observations from the restricted study"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "restricted count"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I do not have access"
        RestCall testRequest = new RestCall(PATH_COUNTS, contentTypeForJSON);
        testRequest.query = toQuery([type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"])

        when: "I count observations for the concept Heart Rate"
        def responseData = get(testRequest)

        then: "I get a count excluding observations from the restricted study"
        assert responseData.count == 5
    }

    /**
     *  given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
     *  when: "I count observations for the concept Heart Rate"
     *  then: "I get a count including observations from the restricted study"
     */
    @Requires({SHARED_CONCEPTS_RESTRICTED_LOADED})
    def "unrestricted count"() {
        given: "Study SHARED_CONCEPTS_RESTRICTED_LOADED is loaded, and I have access"
        setUser(UNRESTRICTED_USERNAME, UNRESTRICTED_PASSWORD)
        RestCall testRequest = new RestCall(PATH_COUNTS, contentTypeForJSON);
        testRequest.query = toQuery([type: ConceptConstraint, path: "\\Vital Signs\\Heart Rate\\"])

        when: "I count observations for the concept Heart Rate"
        def responseData = get(testRequest)

        then: "I get a count including observations from the restricted study"
        assert responseData.count == 7
    }

}
