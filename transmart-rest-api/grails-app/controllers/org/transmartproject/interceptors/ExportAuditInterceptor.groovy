package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class ExportAuditInterceptor extends AuditInterceptor {

    ExportAuditInterceptor() {
        match(controller: ~/export/,
                action: ~/createJob|run|cancel|delete|get|download|jobStatus|dataFormats|fileFormats/)
    }

    boolean after() {
        report('Export job operation', "User (IP: ${IP}) made an export request.")
    }

}
