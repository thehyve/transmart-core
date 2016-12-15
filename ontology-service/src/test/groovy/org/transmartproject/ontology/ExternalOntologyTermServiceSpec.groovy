package org.transmartproject.ontology

import grails.test.mixin.TestFor
import org.apache.tools.ant.taskdefs.optional.extension.Specification


/**
 * Created by ewelina on 14-12-16.
 */
@TestFor(DefaultExternalOntologyTermService)
class ExternalOntologyTermServiceSpec extends Specification {

    def setup() {

    }

    void "test server response wrapping"() {
        when:
        service.wrapOntologyServerResponse()

        then:
        assert 1 == 1
    }
}



