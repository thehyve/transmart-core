package com.recomdata.transmart.rmodules

import org.transmartproject.core.users.User

class AnalysisFilesInterceptor {

    def accessLogService
    User currentUserBean

    AnalysisFilesInterceptor() {
        match(controller: 'analysisFiles', action:'download')
    }

    boolean before() { true }

    boolean after() {
        if (params.path.toLowerCase().endsWith('.zip')) {
            def ip = request.getHeader('X-FORWARDED-FOR') ?: request.remoteAddr
            accessLogService.report(currentUserBean, 'Raw R Data Export',
                    eventMessage: "User (IP: ${ip}) downloaded ${params.path} for ${params.analysisName}",
                    requestURL: "${request.forwardURI}${request.queryString ? '?' + request.queryString : ''}")
        }
        true
    }

    void afterView() {
        // no-op
    }
}
