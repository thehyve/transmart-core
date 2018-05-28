package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class ConceptAuditInterceptor extends AuditInterceptor {

    ConceptAuditInterceptor() {
        match(controller: ~/concept/, action: ~/index|show/)
    }

    boolean after() {
        def ip = getIP()
        report('Concepts retrieval', "User (IP: ${ip}) made a concept request.")
    }
}
