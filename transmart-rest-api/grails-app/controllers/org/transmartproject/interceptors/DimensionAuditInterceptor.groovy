package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class DimensionAuditInterceptor extends AuditInterceptor {

    DimensionAuditInterceptor() {
        match(controller: ~/dimension/, action:~/list/)
    }

    boolean after() {
        def ip = getIP()
        report('Dimension elements retrieval', "User (IP: ${ip}) made a dimension elements request.")
    }

}
