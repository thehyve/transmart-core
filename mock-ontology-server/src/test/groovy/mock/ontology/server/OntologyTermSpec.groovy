package mock.ontology.server


import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(OntologyTermController)
class OntologyTermSpec extends Specification {

    def conceptCode1

    def setup() {
        conceptCode1 = "testCode"
    }

    void "test return conceptCode list"() {
        when:
        def expectedResult = (1..controller.RESPONSE_SIZE).collect{"$conceptCode1 recommended_$it"}
        params.conceptCode = conceptCode1
        controller.index()

        then:
        response.getJson().size() == controller.RESPONSE_SIZE
        response.getJson() == expectedResult
    }
}
