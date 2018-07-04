package org.transmartproject.interceptors

import groovy.transform.CompileStatic
import org.transmartproject.core.binding.BindingHelper
import org.transmartproject.core.log.AccessLogEntryResource
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
        report("$controllerName request", eventMessage)
    }

    protected boolean report(String event, String eventMessage) {

        accessLogService.report(
                authContext.user,
                event,
                eventMessage: eventMessage as Object,
                requestURL: url as Object)

        return true
    }

    protected String getIP() {
        return request.getHeader('X-FORWARDED-FOR') ?: request.remoteAddr
    }

    protected String getUrl() {
        return "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}"
    }

    protected String getEventMessage() {
        Map<String, Object> message = [
                ip    : IP as Object,
                action: actionName as Object
        ]
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            message.put("body", request.JSON as Map)
        }

        return BindingHelper.objectMapper.writeValueAsString(message)
    }
}
