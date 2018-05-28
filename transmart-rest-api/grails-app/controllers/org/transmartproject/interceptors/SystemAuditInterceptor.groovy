package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class SystemAuditInterceptor extends AuditInterceptor {

    SystemAuditInterceptor() {
        match(controller: ~/system/, action: ~/afterDataLoadingUpdate/)
    }

    boolean after() {
        def ip = getIP()
        report("After data loading update ",
                "User (IP: ${ip}) requested clearing of tree node, counts caches, " +
                        "patient sets and bitsets, update of data for subscribed user queries.")
    }

}
