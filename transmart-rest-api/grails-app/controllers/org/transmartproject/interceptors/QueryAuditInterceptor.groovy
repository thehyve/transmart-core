package org.transmartproject.interceptors

import groovy.transform.CompileStatic

@CompileStatic
class QueryAuditInterceptor extends AuditInterceptor {

    QueryAuditInterceptor() {
        match(controller: ~/query/,
                action: ~/observations|counts|countsPerConcept|countsPerStudy|countsPerStudyAndConcept|aggregatesPerConcept|crosstable|table|supportedFields/)
    }

    boolean after() {
        report("Observations data related call.", "User (IP: $IP) made an observation data request. Action: $actionName")
    }

}
