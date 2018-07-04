package org.transmartproject.rest.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.LoggingEvent
import org.slf4j.LoggerFactory
import org.transmartproject.core.binding.BindingHelper
import spock.lang.Specification

class ApiAuditLogJsonLayoutSpec extends Specification {

    static LoggingEvent makeEvent(String msg) {
        Logger logger=(Logger)LoggerFactory.getLogger(this.getClass())
        new LoggingEvent("", logger, Level.DEBUG, msg, null, null)
    }

    void testBasicJsonLayoutMapping() {
        def layout = new ApiAuditLogJsonLayout()

        when:
        Map<String, String> messageMap = [
                "username"    : "testUser",
                "event"       : "test request",
                "eventMessage": '{"ip":"127.0.0.1","action":"testAction"}',
                "requestURL"  : "/v2/test",
                "accessTime"  : "2018-07-04T14:29:35"

        ]
        String message = BindingHelper.objectMapper.writeValueAsString(messageMap)
        def result = layout.toJsonMap(makeEvent(message))

        then:
        result instanceof Map
        result == messageMap
    }
}
