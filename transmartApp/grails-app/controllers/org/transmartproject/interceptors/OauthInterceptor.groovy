package org.transmartproject.interceptors


class OauthInterceptor {
    //matches OauthController and all actions of controller by naming convention
    def auditLogService
    boolean before() {
        auditLogService.report("OAuth authentication", request,
                user: currentUserBean,
        )
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
