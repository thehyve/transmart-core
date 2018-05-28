package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class ExportAuditInterceptor extends AuditInterceptor {

    ExportAuditInterceptor() {
        match(controller: ~/export/,
                action: ~/createJob|run|cancel|delete|get|download|jobStatus|dataFormats|fileFormats/)
    }

    boolean after() {
        def ip = getIP()
        report('Export job operation', "User (IP: ${ip}) made an export request.")
    }

}
