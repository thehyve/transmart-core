package org.transmartproject.interceptors

import org.transmartproject.core.log.AccessLogEntryResource
import org.transmartproject.core.users.User

class ExportAuditInterceptor {

    AccessLogEntryResource accessLogService
    User currentUserBean

    ExportAuditInterceptor(){
        match(controller: ~/export/).excludes(action: ~/listJobs/)
    }

    boolean after(){
        def fullUrl = "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}"
        def ip = request.getHeader('X-FORWARDED-FOR') ?: request.remoteAddr

        accessLogService.report(currentUserBean, 'Export job operation',
                eventMessage:  "User (IP: ${ip}) made an export request.",
                requestURL: fullUrl)
        true
    }

}
