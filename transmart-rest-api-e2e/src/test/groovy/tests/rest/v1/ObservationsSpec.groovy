package tests.rest.v1

import base.RESTSpec

import spock.lang.Requires

import static config.Config.CELL_LINE_ID
import static config.Config.CELL_LINE_LOADED
import static config.Config.EHR_LOADED
import static config.Config.V1_PATH_STUDIES
import static config.Config.V1_PATH_observations


class ObservationsSpec extends RESTSpec{

    /**
     *  given: "study EHR is loaded"
     *  when: "I request all observations related to a patient and concept"
     *  then: "I get all relevant observations"
     */
    @Requires({EHR_LOADED})
    def "v1 observations by query map"(){
        given: "study EHR is loaded"
        def id = -62
        def path = "\\Public Studies\\EHR\\Demography\\Age\\"

        when: "I request all observations related to a patient and concept"
        def responseData = get([
                path: V1_PATH_observations,
                query: [
                        patients: [id],
                        concept_paths: [path]
                ],
                acceptType: contentTypeForJSON
        ])

        then: "I get all relevant observations"
        responseData.each {
            assert it.label == path
            assert it.subject.id == id
        }
    }

    /**
     *  given: "study CELL-LINE is loaded"
     *  when: "I request all observations related to this study and concept"
     *  then: "I get observations"
     */
    @Requires({CELL_LINE_LOADED})
    def "v1 observations by concept"(){
        given: "study CELL-LINE is loaded"
        def studyId = CELL_LINE_ID
        def conceptPath = 'Molecular profiling/Non-highthroughput molecular profiling/Copy number aberrations (DNA)/MPLA-gain-P146/13q/'
        def path = "studies/${studyId}/concepts/${conceptPath}/observations"

        when: "I request all observations related to this study and concept"
        def responseData = get([path: path, acceptType: contentTypeForJSON])

        then: "I get observations"
        responseData.each {
            assert it.label != null
            assert it.subject != null
        }
    }


    /**
     *  given: "study CELL-LINE is loaded"
     *  when: "I request all observations related to this study"
     *  then: "I get observations"
     */
    @Requires({CELL_LINE_LOADED})
    def "v1 observations by study"(){
        given: "study CELL-LINE is loaded"
        def studyId = CELL_LINE_ID

        when: "I request all observations related to this study"
        def responseData = get([path: V1_PATH_STUDIES+"/${studyId}/observations", acceptType: contentTypeForJSON])

        then: "I get observations"
        responseData.each {
            assert it.label != null
            assert it.subject != null
        }
    }

}
