package tests.rest.v1

import base.RESTSpec
import base.RestCall
import spock.lang.Requires

import static config.Config.EHR_ID
import static config.Config.EHR_LOADED
import static config.Config.V1_PATH_STUDIES

@Requires({EHR_LOADED})
class ConceptsSpec extends RESTSpec{

    /**
     *  given: "study EHR is loaded"
     *  when: "I request all concepts related to this study"
     *  then: "I get all relevant concepts"
     */
    def "v1 all concepts"(){
        given: "study EHR is loaded"
        def studieId = EHR_ID
        RestCall testRequest = new RestCall(V1_PATH_STUDIES+"/${studieId}/concepts", contentTypeForJSON);

        when: "I request all concepts related to this study"
        def responseData = get(testRequest)

        then: "I get all relevant concepts"
        responseData.ontology_terms.each {
            assert it.fullName != null
            assert it.key != null
        }
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "I request one concept related to this study"
     *  then: "I get the requested concept"
     */
    def "v1 single concept"(){
        given: "study EHR is loaded"
        def studieId = EHR_ID
        def conceptPath = "Demography/Age/"
        RestCall testRequest = new RestCall(V1_PATH_STUDIES+"/${studieId}/concepts/${conceptPath}", contentTypeForJSON);

        when: "I request one concept related to this study"
        def responseData = get(testRequest)

        then: "I get the requested concept"
        assert responseData.name == "Age"
        assert responseData.fullName == "\\Public Studies\\EHR\\Demography\\Age\\"
        assert responseData.key == "\\\\Public Studies\\Public Studies\\EHR\\Demography\\Age\\"
        assert responseData.type == "NUMERIC"
    }
}
