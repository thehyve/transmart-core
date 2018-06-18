package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class DimensionAuditInterceptor extends AuditInterceptor {

    DimensionAuditInterceptor() {
        match(controller: ~/dimension/, action:~/list/)
    }

    boolean after() {
        report('Dimension elements retrieval', "User (IP: ${IP}) made a dimension elements request.")
    }

}
