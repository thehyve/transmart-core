package org.transmartproject.interceptors

import groovy.transform.CompileStatic
import org.transmartproject.core.log.AccessLogEntryResource
import org.transmartproject.core.users.User
import org.transmartproject.rest.user.AuthContext

@CompileStatic
abstract class AuditInterceptor {

    AccessLogEntryResource accessLogService
    AuthContext authContext

    boolean before() { true }

    boolean after() { true }

    protected String getUrl() {
        return "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}"
    }

    protected boolean report(String event, String eventMessage) {
        def fullUrl = getUrl()
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            eventMessage += " Request body: ${request.JSON}."
        }

        accessLogService.report(
                authContext.user,
                event,
                eventMessage: eventMessage as Object,
                requestURL: fullUrl as Object)

        return true
    }

    protected String getIP() {
        return request.getHeader('X-FORWARDED-FOR') ?: request.remoteAddr
    }
}
