/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package mock.ontology.server

import grails.test.mixin.TestFor
import spock.lang.Specification

import static mock.ontology.server.OntologyTermResponseGenerator.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(OntologyTermController)
class OntologyTermSpec extends Specification {

    String contentTypeForJSON

    def setup() {
        contentTypeForJSON = 'application/json'
    }

    void "test '/search/conceptCode' endpoint"() {
        when:
        params.conceptCode = "default_code"
        controller.index()
        def jsonResponse = response.getJson()

        then:
        response.contentType.contains(contentTypeForJSON)
        jsonResponse.size() == 2
        jsonResponse.each {
            assert it.has('score')
            assert it.has('classpath')
            assert it.has('terminology_type')
            assert it.has('label')
        }
        jsonResponse[0].classpath as List<List<String>> == defaultClasspath
        jsonResponse[1].classpath as List<List<String>> == snomedClasspath

        when:
        params.conceptCode = mainSnomedCode
        response.reset()
        controller.index()
        jsonResponse = response.getJson()

        then:
        response.contentType.contains(contentTypeForJSON)
        jsonResponse.size() == 2
        jsonResponse.each {
            assert it.has('score')
            assert it.has('classpath')
            assert it.has('terminology_type')
            assert it.has('label')
        }
        jsonResponse[0].classpath as List<List<String>> == snomedClasspath
        jsonResponse[1].classpath as List<List<String>> == defaultClasspath
    }

    void "test '/idx' endpoint"(){
        when:
        def code = "roxId"
        params.roxId = code
        controller.show()
        def jsonResponse = response.getJson()

        then:
        response.contentType.contains(contentTypeForJSON)
        jsonResponse.size() == 3
        jsonResponse.has('id')
        jsonResponse.has('node')
        jsonResponse.has('type')
        jsonResponse.id == code
        jsonResponse.node == 'Default label'
        jsonResponse.type == sampleDescriptionsMap['Default label']

        when:
        code = mainDefaultCode
        params.roxId = code
        response.reset()
        controller.show()
        jsonResponse = response.getJson()

        then:
        response.contentType.contains(contentTypeForJSON)
        jsonResponse.size() == 3
        jsonResponse.has('id')
        jsonResponse.has('node')
        jsonResponse.has('type')
        jsonResponse.id == code
        jsonResponse.node == labels[mainDefaultCode]
        jsonResponse.type == sampleDescriptionsMap[labels[mainDefaultCode]]
    }
}

