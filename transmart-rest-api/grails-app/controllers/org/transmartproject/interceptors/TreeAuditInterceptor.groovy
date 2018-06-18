package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class TreeAuditInterceptor extends AuditInterceptor {

    TreeAuditInterceptor() {
        match(controller: ~/tree/, action: ~/index|clearCache|rebuildCache|rebuildStatus/)
    }

    boolean after() {
        if (actionName == 'index') {
            return report('Tree nodes retrieval',
                    "User (IP: ${IP}) made a tree nodes request.")
        } else {
            report('Tree nodes and count cashes update', "User (IP: ${IP}) made a tree cashe request.")
        }
    }

}
