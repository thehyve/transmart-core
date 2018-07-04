package org.transmartproject.interceptors

import groovy.transform.CompileStatic
import org.transmartproject.core.log.AccessLogEntryResource
import org.transmartproject.core.users.User
import org.transmartproject.rest.user.AuthContext

@CompileStatic
class ApiAuditInterceptor {

    AccessLogEntryResource accessLogService
    AuthContext authContext

    ApiAuditInterceptor() {
        match(controller: ~/arvados/)
        match(controller: ~/concept/)
        match(controller: ~/config/)
        match(controller: ~/dimension/)
        match(controller: ~/export/).excludes(action: ~/listJobs/)
        match(controller: ~/patientQuery/)
        match(controller: ~/relationType/)
        match(controller: ~/storage/)
        match(controller: ~/storageSystem/)
        match(controller: ~/query/)
        match(controller: ~/studyQuery/)
        match(controller: ~/system/)
        match(controller: ~/tree/)
        match(controller: ~/userQuery/)
        match(controller: ~/userQuerySet/)
        match(controller: ~/version/)
    }

    boolean before() { true }

    boolean after() {
        report("$controllerName request.",
                "User (IP: ${IP}) made a $controllerName request. Action: $actionName")
    }

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
