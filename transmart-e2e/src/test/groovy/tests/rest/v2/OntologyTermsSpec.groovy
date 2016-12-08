package tests.rest.v2

import base.RESTSpec
import groovy.json.JsonBuilder
import spock.lang.Requires

import static tests.rest.v2.QueryType.AVERAGE
import static tests.rest.v2.QueryType.MAX
import static tests.rest.v2.QueryType.MIN
import static tests.rest.v2.constraints.ConceptConstraint
import static config.Config.*
/**
 * Created by ewelina on 8-12-16.
 */
class OntologyTermsSpec extends RESTSpec {

    @Requires({"Ontology Server Running"})//For now mock-ontology-server app running
    def "get recommended concept codes from external server"() {
        given: "external ontology server is running"

        when:
        def conceptCode = "test_code"
        def responseData = get("$PATH_RECOMMENTED_CONCEPTS/$conceptCode", contentTypeForJSON)

        then:
        responseData.size() > 0
    }
}
