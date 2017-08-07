package org.transmartproject.interceptors

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.audit.AuditLogger
import org.transmartproject.core.users.User


class RWGInterceptor {

    @Autowired(required=false)
    AuditLogger auditLogService
    @Autowired
    User currentUserBean

    RWGInterceptor(){
        match(controller: 'RWG', action: 'getFacetResults')
    }
    boolean before() {
        auditLogService.report("Clinical Data Active Filter", request,
        query: params.searchTerms,
        user: currentUserBean,
        )
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
