package org.transmartproject.rest

import org.transmartproject.core.users.User

class AuditLogFilters {

    def accessLogService
    User currentUserBean

    def filters = {
        lowDim(controller: 'observation', action:'*') {
            after = { model ->
                def fullUrl = "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}"

                accessLogService.report(currentUserBean, 'REST API Data Retrieval',
                        eventMessage:  "User (IP: ${request.remoteAddr}) got low dim. data with ${fullUrl}",
                        requestURL: fullUrl)
            }
        }

        highDim(controller: 'highDim', action:'*') {
            after = { model ->
                def fullUrl = "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}"

                accessLogService.report(currentUserBean, 'REST API Data Retrieval',
                        eventMessage:  "User (IP: ${request.remoteAddr}) got high dim. data with ${fullUrl}",
                        requestURL: fullUrl)
            }
        }
    }

}
