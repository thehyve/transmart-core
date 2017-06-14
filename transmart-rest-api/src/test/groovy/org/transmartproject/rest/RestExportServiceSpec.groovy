package org.transmartproject.rest

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(RestExportService)
class RestExportServiceSpec extends Specification {
//
//    @Autowired
//    RestDataExportService restDataExportService
    
    def setup() {
    }

    def cleanup() {
    }

    void "test something"() {
        service.export('test', [types: [dataType:'clinical', format:'TSV'], ids: [28838,28837]], 'admin')
        expect:"fix me"
        true == true
    }
}
