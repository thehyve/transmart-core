package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class ConceptAuditInterceptor extends AuditInterceptor {

    ConceptAuditInterceptor() {
        match(controller: ~/concept/, action: ~/index|show/)
    }

    boolean after() {
        report('Concepts retrieval', "User (IP: ${IP}) made a concept request.")
    }
}
