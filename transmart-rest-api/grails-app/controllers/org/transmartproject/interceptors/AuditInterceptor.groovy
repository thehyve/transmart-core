package org.transmartproject.interceptors

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.binding.BindingHelper
import org.transmartproject.core.log.AccessLogEntryResource
import org.transmartproject.rest.user.AuthContext

@CompileStatic
@Slf4j
abstract class AuditInterceptor {

    static final String AUDIT_START_TIME_ATTRIBUTE = 'transmartAuditStartTime'

    @Autowired
    AccessLogEntryResource accessLogEntryResource

    @Autowired
    AuthContext authContext

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

    /**
     * Request handling time in milliseconds.
     */
    protected Long getDuration() {
        def startDate = (Date)request.getAttribute(AUDIT_START_TIME_ATTRIBUTE)
        if (!startDate) {
            return null
        }
        new Date().time - startDate.time
    }

    protected String getEventMessage() {
        Map<String, Object> message = [
                ip      : (Object)ip,
                action  : (Object)actionName,
                duration: (Object)duration,
                status  : (Object)response.status.toInteger()
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
