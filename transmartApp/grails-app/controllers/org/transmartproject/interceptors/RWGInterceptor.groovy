package org.transmartproject.interceptors

import org.transmartproject.core.users.User


class RWGInterceptor {
    def auditLogService
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
