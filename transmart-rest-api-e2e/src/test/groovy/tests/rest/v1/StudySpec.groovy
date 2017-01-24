package tests.rest.v1

import base.RESTSpec
import base.RestCall
import spock.lang.Requires

import static config.Config.ADMIN_PASSWORD
import static config.Config.ADMIN_USERNAME
import static config.Config.EHR_HIGHDIM_LOADED
import static config.Config.EHR_LOADED
import static config.Config.PATH_AGGREGATE
import static config.Config.V1_PATH_STUDIES
import static tests.rest.v2.QueryType.MAX
import static tests.rest.v2.constraints.ConceptConstraint

@Requires({EHR_LOADED})
class StudySpec extends RESTSpec{

    /**
     *  given: "several studies are loaded"
     *  when: "I request all studies"
     *  then: "I get all studies I have access to"
     */
    @Requires({EHR_HIGHDIM_LOADED})
    def "v1 all studies"(){
        given: "several studies are loaded"
        setUser(ADMIN_USERNAME,ADMIN_PASSWORD)

        when: "I request all studies"
        def responseData = get([
                path: V1_PATH_STUDIES,
                acceptType: contentTypeForJSON
        ])

        then: "I get several studies"
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
        setUser(ADMIN_USERNAME,ADMIN_PASSWORD)
        def studieId = 'EHR'

        when: "I request studies with id EHR"
        def responseData = get([
                path: V1_PATH_STUDIES+"/${studieId}",
                acceptType: contentTypeForJSON
        ])

        then: "only the EHR study is returned"
        assert responseData.id == 'EHR'
        assert responseData.ontologyTerm.fullName == "\\Public Studies\\EHR\\"
        assert responseData.ontologyTerm.key == '\\\\Public Studies\\Public Studies\\EHR\\'
        assert responseData.ontologyTerm.name == 'EHR'
        assert responseData.ontologyTerm.type == 'STUDY'
    }
}
