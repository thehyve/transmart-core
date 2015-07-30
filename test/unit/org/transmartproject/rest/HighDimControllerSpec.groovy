package org.transmartproject.rest

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.apache.log4j.Appender
import org.apache.log4j.LogManager
import org.apache.log4j.spi.LoggingEvent
import spock.lang.Specification

@TestFor(HighDimController)
@Mock(AuditLogFilters)
class HighDimControllerSpec extends Specification {

    void "test index actions is logged"() {
        when:
        def logger = LogManager.getLogger('grails.app.filters.org.transmartproject.rest.AuditLogFilters')
        def logEvents = []
        logger.addAppender([doAppend: { LoggingEvent event -> logEvents << event }] as Appender)

        controller.metaClass.index = { -> }
        request.remoteUser = 'testUser'
        request.forwardURI = '/test/uri/'
        request.queryString = 'foo=bar'
        request.remoteAddr = '127.0.0.1'

        withFilters(action: "index") {
            controller.index()
        }
        then:
        logEvents.size() == 1
        logEvents[0].message == 'testUser (IP: 127.0.0.1) gets high dim. data with /test/uri/?foo=bar'
    }

}
