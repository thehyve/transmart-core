package org.transmartproject.interceptors

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.binding.BindingHelper
import org.transmartproject.core.log.AccessLogEntryResource
import org.transmartproject.rest.user.AuthContext

@CompileStatic
@Slf4j
class ApiAuditInterceptor {

    @Autowired
    AccessLogEntryResource accessLogEntryResource

    @Autowired
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

    boolean after() {
        report("$controllerName request", eventMessage)
    }

    protected boolean report(String event, String eventMessage) {
        accessLogEntryResource.report(
                authContext.user,
                event,
                eventMessage: (Object)eventMessage,
                requestURL: (Object)url)
        return true
    }

    protected String getIp() {
        return request.getHeader('X-FORWARDED-FOR') ?: request.remoteAddr
    }

    protected String getUrl() {
        return "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}"
    }

    protected String getEventMessage() {
        Map<String, Object> message = [
                ip    : (Object)ip,
                action: (Object)actionName,
                status: (Object)response.status.toInteger()
        ]
        if (request.isPost()) {
            try {
                message.put("body", request.JSON as Map)
            } catch (IllegalStateException e) {
                log.error "Cannot read body for request ${url}: ${e.message}" +
                        "\nTry to use request.inputStream instead of request.reader."
            }
        }

        return BindingHelper.objectMapper.writeValueAsString(message)
    }

}
