package tests.rest.v1

import base.RESTSpec

import spock.lang.Requires

import static config.Config.ADMIN_PASSWORD
import static config.Config.ADMIN_USERNAME
import static config.Config.EHR_HIGHDIM_LOADED
import static config.Config.EHR_LOADED
import static config.Config.GSE8581_ID
import static config.Config.V1_PATH_STUDIES

@Requires({GSE8581_ID})
class StudySpec extends RESTSpec{

    /**
     *  given: "several studies are loaded"
     *  when: "I request all studies"
     *  then: "I get all studies I have access to"
     */
    def "v1 all studies"(){
        given: "several studies are loaded"

        when: "I request all studies"
        def responseData = get([
                path: V1_PATH_STUDIES,
                acceptType: contentTypeForJSON
        ])

        then: "I get several studies"
        def studies = responseData.studies as List
        def studyIds = studies*.id as List

        assert studyIds.contains(GSE8581_ID)
        responseData.studies.each {
            assert it.ontologyTerm != null
        }
    }

    /**
     *  given: "a study with id EHR is loaded"
     *  when: "I request studies with id EHR"
     *  then: "only the EHR study is returned"
     */
    def "v1 single study"(){
        given: "study EHR is loaded"
        def studieId = GSE8581_ID

        when: "I request studies with id EHR"
        def responseData = get([
                path: V1_PATH_STUDIES+"/${studieId}",
                acceptType: contentTypeForJSON
        ])

        then: "only the EHR study is returned"
        assert responseData.id == GSE8581_ID
        assert responseData.ontologyTerm.fullName == "\\Public Studies\\${studieId}\\"
        assert responseData.ontologyTerm.key == "\\\\Public Studies\\Public Studies\\${studieId}\\"
        assert responseData.ontologyTerm.name == studieId
        assert responseData.ontologyTerm.type == 'STUDY'
    }
}
