package org.transmartproject.rest.logging

import groovy.transform.CompileStatic

/**
 * Representation of logs of the REST API calls
 */
@CompileStatic
class ApiAuditLogRepresentation {

    String username
    String event
    String eventMessage
    String requestURL
    String accessTime
}
