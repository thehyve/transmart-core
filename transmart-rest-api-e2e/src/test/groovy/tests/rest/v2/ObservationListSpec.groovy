package tests.rest.v2

import base.RESTSpec
import base.RestCall
import spock.lang.Requires

import static config.Config.EHR_LOADED
import static config.Config.PATH_COUNTS
import static config.Config.PATH_OBSERVATION_LIST
import static tests.rest.v2.constraints.ConceptConstraint

class ObservationListSpec extends RESTSpec {

    /**
     *  given: "study EHR is loaded"
     *  when: "for that study I request the observations as a list"
     *  then: "I get all observations in list form"
     */
    @Requires({EHR_LOADED})
    def "observations as list"(){
        given: "study EHR is loaded"
        def request = [
                path: PATH_OBSERVATION_LIST,
                acceptType: contentTypeForJSON,
                query: toQuery([
                        type: ConceptConstraint,
                        path:"\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                ])
        ]

        when: "for that study I request the observations as a list"
        def responseData = get(request)

        then: "I get all observations in list form"
        responseData.each {
            assert it.conceptCode == "EHR:VSIGN:HR"
        }
    }
}