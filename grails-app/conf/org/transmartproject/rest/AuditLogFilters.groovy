package org.transmartproject.rest

class AuditLogFilters {

    def filters = {
        lowDim(controller: 'observation', action:'*') {
            after = { model ->
                log.info("${request.remoteUser} (IP: ${request.remoteAddr}) gets low dim. data" +
                        " with ${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}")
            }
        }

        highDim(controller: 'highDim', action:'*') {
            after = { model ->
                log.info("${request.remoteUser} (IP: ${request.remoteAddr}) gets high dim. data" +
                        " with ${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}")
            }
        }
    }
}
