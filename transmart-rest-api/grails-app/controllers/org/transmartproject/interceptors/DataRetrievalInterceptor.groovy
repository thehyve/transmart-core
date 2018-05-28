package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class DataRetrievalInterceptor extends AuditInterceptor {

    DataRetrievalInterceptor() {
        match(controller: ~/(observation|highDim)/)
    }

    boolean before() { true }

    boolean after() {
        def ip = getIP()
        def fullUrl = getUrl()
        def dataType = controllerName == 'observation' ? 'low dim' : 'high dim'

        report('REST API Data Retrieval', "User (IP: ${ip}) got ${dataType}. data with ${fullUrl}")
    }

}
