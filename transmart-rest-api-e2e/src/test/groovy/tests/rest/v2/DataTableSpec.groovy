package tests.rest.v2

import base.RESTSpec
import groovy.util.logging.Slf4j

import static config.Config.PATH_DATA_TABLE
import static tests.rest.constraints.ConceptConstraint

@Slf4j
class DataTableSpec extends RESTSpec {

    def "get data table"() {
        def params = [
                constraint: toJSON([
                        type: ConceptConstraint,
                        path: "\\Public Studies\\EHR\\Vital Signs\\Heart Rate\\"
                ]),
                //TODO type clinical?
        ]
        def request = [
                path: PATH_DATA_TABLE,
                //TODO accept type?
        ]

        when:
        def responseData = getOrPostRequest(method, request, params)
        then:
        responseData

        where:
        method | _
        "POST" | _
        "GET"  | _
    }

}
