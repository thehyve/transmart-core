package org.transmartproject.db.querytool

import grails.test.mixin.TestFor
import org.transmartproject.core.querytool.QueryDefinition
import spock.lang.Specification

@TestFor(QueriesResourceService)
class QueriesResourceServiceSpec extends Specification {

    def setup() {
        grailsApplication.config.clear()
    }

    void testI2b2UserNotSpecified() {
        grailsApplication.config.org.transmartproject.i2b2.user_id = null
        def queryDefinition = new QueryDefinition([])

        when:
        service.runQuery(queryDefinition)

        then:
        def e = thrown(AssertionError)
        e.message.contains('org.transmartproject.i2b2.user_id is not specified.')
    }
}
