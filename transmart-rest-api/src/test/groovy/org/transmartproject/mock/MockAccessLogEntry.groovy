package org.transmartproject.mock

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.transmartproject.core.log.AccessLogEntry

@Canonical
@CompileStatic
class MockAccessLogEntry implements AccessLogEntry {

    Long id
    String username
    String event
    String eventMessage
    String requestURL
    Date accessTime

}
