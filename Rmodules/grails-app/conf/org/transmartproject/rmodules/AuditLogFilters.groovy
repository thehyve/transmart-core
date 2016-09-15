package org.transmartproject.rmodules

import org.transmartproject.core.users.User

class AuditLogFilters {

    def accessLogService
    User currentUserBean

    def filters = {
        download(controller: 'analysisFiles', action:'download') {
            after = { model ->
                if (params.path.toLowerCase().endsWith('.zip')) {
                    def ip = request.getHeader('X-FORWARDED-FOR') ?: request.remoteAddr
                    accessLogService.report(currentUserBean, 'Raw R Data Export',
                            eventMessage: "User (IP: ${ip}) downloaded ${params.path} for ${params.analysisName}",
                            requestURL: "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}")
                }
            }
        }
    }

}
