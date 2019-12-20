package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class DataRetrievalInterceptor extends AuditInterceptor {

    // /v1 interceptor
    DataRetrievalInterceptor() {
        match(controller: ~/(observation|highDim)/)
    }

    boolean before() { true }

    boolean after() {
        def dataType = controllerName == 'observation' ? 'low dim' : 'high dim'

        report('REST API Data Retrieval', "User (IP: ${ip}) got ${dataType}. data with ${url}")
    }

}
