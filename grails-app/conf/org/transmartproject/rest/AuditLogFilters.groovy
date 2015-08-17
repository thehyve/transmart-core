package org.transmartproject.rest

import org.transmartproject.core.users.User

class AuditLogFilters {

    def accessLogService
    User currentUserBean

    def filters = {
        lowDim(controller: 'observation', action:'*') {
            after = { model ->
                def fullUrl = "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}"
                def ip = request.getHeader('X-FORWARDED-FOR') ?: request.remoteAddr

                accessLogService.report(currentUserBean, 'REST API Data Retrieval',
                        eventMessage:  "User (IP: ${ip}) got low dim. data with ${fullUrl}",
                        requestURL: fullUrl)
            }
        }

        highDim(controller: 'highDim', action:'*') {
            after = { model ->
                def fullUrl = "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}"
                def ip = request.getHeader('X-FORWARDED-FOR') ?: request.remoteAddr

                accessLogService.report(currentUserBean, 'REST API Data Retrieval',
                        eventMessage:  "User (IP: ${ip}) got high dim. data with ${fullUrl}",
                        requestURL: fullUrl)
            }
        }
    }

}
