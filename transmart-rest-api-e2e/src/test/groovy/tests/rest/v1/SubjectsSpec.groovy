package tests.rest.v1

import base.RESTSpec

import spock.lang.Requires

import static config.Config.ADMIN_PASSWORD
import static config.Config.ADMIN_USERNAME
import static config.Config.EHR_ID
import static config.Config.EHR_LOADED
import static config.Config.V1_PATH_STUDIES

@Requires({EHR_LOADED})
class SubjectsSpec extends RESTSpec{

    /**
     *  given: "study EHR is loaded"
     *  when: "I request subjects related to this study"
     *  then: "subjects are returned"
     */
    def "v1 subjects"(){
        given: "study EHR is loaded"
        setUser(ADMIN_USERNAME,ADMIN_PASSWORD)
        def studyId = EHR_ID

        when: "I request subjects related to this study"
        def responseData = get([
                path: V1_PATH_STUDIES+"/${studyId}/subjects",
                acceptType: contentTypeForJSON
        ])

        then: "subjects are returned"
        assert responseData.subjects.size() == 3
        responseData.subjects.each {
            assert [-62,-52,-42].contains(it.id)
            assert it.trial == studyId
        }
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "I request a subject by it's id"
     *  then: "only that subject is returned"
     */
    def "v1 single subject"(){
        given: "study EHR is loaded"
        setUser(ADMIN_USERNAME, ADMIN_PASSWORD)
        def studyId = EHR_ID
        def subjectId = -62

        when: "I request a subject by it's id"
        def responseData = get([
                path: V1_PATH_STUDIES+"/${studyId}/subjects/${subjectId}",
                acceptType: contentTypeForJSON
        ])

        then: "only that subject is returned"
        assert responseData.id == subjectId
    }

    /**
     *  given: "study EHR is loaded"
     *  when: "I request subjects related to this study and a concept path"
     *  then: "subjects are returned"
     */
    def "v1 subjects by concept"(){
        given: "study EHR is loaded"
        setUser(ADMIN_USERNAME,ADMIN_PASSWORD)
        def studyId = EHR_ID
        def conceptPath = "Demography/Age/"

        when: "I request subjects related to this study and a concept path"
        def responseData = get([
                path: V1_PATH_STUDIES+"/${studyId}/concepts/${conceptPath}/subjects",
                acceptType: contentTypeForJSON
        ])

        then: "subjects are returned"
        assert responseData.subjects.size() == 3
        responseData.subjects.each {
            assert [-62,-52,-42].contains(it.id)
            assert it.trial == studyId
        }
    }
}
