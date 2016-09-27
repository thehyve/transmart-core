package org.transmartproject.interceptors

import org.transmartproject.core.users.User

class DataRetrievalInterceptor {
    def accessLogService
    User currentUserBean

    DataRetrievalInterceptor(){
        match(controller: ~/(observation|highDim)/)
    }

    boolean before() {true}

    boolean after(){
        def fullUrl = "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}"
        def ip = request.getHeader('X-FORWARDED-FOR') ?: request.remoteAddr

        def dataType = controllerName == 'observation' ? 'low dim' : 'high dim'

        accessLogService.report(currentUserBean, 'REST API Data Retrieval',
                eventMessage:  "User (IP: ${ip}) got ${dataType}. data with ${fullUrl}",
                requestURL: fullUrl)
        true
    }

    void afterView(){}
}
