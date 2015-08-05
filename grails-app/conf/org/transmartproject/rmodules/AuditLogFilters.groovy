package org.transmartproject.rmodules

import org.transmartproject.core.users.User

class AuditLogFilters {

    def accessLogService
    User currentUserBean

    def filters = {
        download(controller: 'analysisFiles', action:'download') {
            after = { model ->
                if (params.path.toLowerCase().endsWith('.zip')) {
                    accessLogService.report(currentUserBean, 'Raw R Data Export',
                            eventMessage: "User (IP: ${request.remoteAddr}) downloaded ${params.path} for ${params.analysisName}",
                            requestURL: "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}")
                }
            }
        }
    }

}
