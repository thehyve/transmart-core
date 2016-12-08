package mock.ontology.server


import grails.test.mixin.TestFor
import spock.lang.Specification

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
        params.conceptCode = "testCode"
        controller.index()

        then:
        response.contentType.contains(contentTypeForJSON)
        response.getJson().size() > 0
        response.getJson().each {
            assert it.has('score')
            assert it.has('classpath')
            assert it.has('terminology_type')
            assert it.has('label')
        }
    }

    void "test '/idx' endpoint"(){
        when:
        params.conceptCode = "roxId"
        controller.show()

        then:
        response.contentType.contains(contentTypeForJSON)
        response.getJson().size() > 0
        response.getJson().each {
            assert it.has('id')
            assert it.has('node')
            assert it.has('type')
        }
    }
}

