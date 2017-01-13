package tests.rest.v1

import base.RESTSpec
import base.RestCall
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
        RestCall testRequest = new RestCall(V1_PATH_observations, contentTypeForJSON);
        def id = -62
        def path = "\\Public Studies\\EHR\\Demography\\Age\\"
        testRequest.query = [
                patients: [id],
                concept_paths: [path]
        ]

        when: "I request all observations related to a patient and concept"
        def responseData = get(testRequest)

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

        RestCall testRequest = new RestCall(path, contentTypeForJSON);

        when: "I request all observations related to this study and concept"
        def responseData = get(testRequest)

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
        RestCall testRequest = new RestCall(V1_PATH_STUDIES+"/${studyId}/observations", contentTypeForJSON);

        when: "I request all observations related to this study"
        def responseData = get(testRequest)

        then: "I get observations"
        responseData.each {
            assert it.label != null
            assert it.subject != null
        }
    }

}
