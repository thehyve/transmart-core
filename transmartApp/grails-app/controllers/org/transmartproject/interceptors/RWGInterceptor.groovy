package org.transmartproject.interceptors


class RWGInterceptor {
    def auditLogService
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
