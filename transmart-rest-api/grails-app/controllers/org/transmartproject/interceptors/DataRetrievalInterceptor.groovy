package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class DataRetrievalInterceptor extends ApiAuditInterceptor {

    // /v1 interceptor
    DataRetrievalInterceptor() {
        match(controller: ~/(observation|highDim)/)
    }

    boolean before() { true }

    boolean after() {
        def fullUrl = getUrl()
        def dataType = controllerName == 'observation' ? 'low dim' : 'high dim'

        report('REST API Data Retrieval', "User (IP: ${IP}) got ${dataType}. data with ${fullUrl}")
    }

}
