/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package tests.rest.v1

import annotations.RequiresStudy
import base.RESTSpec

import static base.ContentTypeFor.contentTypeForJSON
import static config.Config.GSE8581_ID
import static config.Config.V1_PATH_STUDIES

@RequiresStudy(GSE8581_ID)
class ConceptsSpec extends RESTSpec {

    /**
     *  given: "study GSE8581 is loaded"
     *  when: "I request all concepts related to this study"
     *  then: "I get all relevant concepts"
     */
    def "v1 all concepts"() {
        given: "study EHR is loaded"
        def studieId = GSE8581_ID

        when: "I request all concepts related to this study"
        def responseData = get([path: V1_PATH_STUDIES + "/${studieId}/concepts", acceptType: contentTypeForJSON])

        then: "I get all relevant concepts"
        responseData.ontology_terms.each {
            assert it.fullName != null
            assert it.key != null
        }
    }

    /**
     *  given: "study GSE8581 is loaded"
     *  when: "I request one concept related to this study"
     *  then: "I get the requested concept"
     */
    def "v1 single concept"() {
        given: "study EHR is loaded"
        def studieId = GSE8581_ID
        def conceptPath = "Subjects/Age/"

        when: "I request one concept related to this study"
        def responseData = get([path: V1_PATH_STUDIES + "/${studieId}/concepts/${conceptPath}", acceptType: contentTypeForJSON])

        then: "I get the requested concept"
        assert responseData.name == "Age"
        assert responseData.fullName == "\\Public Studies\\GSE8581\\Subjects\\Age\\"
        assert responseData.key == "\\\\Public Studies\\Public Studies\\GSE8581\\Subjects\\Age\\"
        assert responseData.type == "NUMERIC"
    }
}
