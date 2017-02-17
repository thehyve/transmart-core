package org.transmart.server.interceptors

import org.transmartproject.core.users.User


class OauthInterceptor {
    //matches OauthController and all actions of controller by naming convention
    def auditLogService
    User currentUserBean
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
