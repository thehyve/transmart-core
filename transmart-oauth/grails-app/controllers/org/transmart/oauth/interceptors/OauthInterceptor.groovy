package org.transmart.oauth.interceptors

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.audit.AuditLogger
import org.transmartproject.core.users.User


class OauthInterceptor {
    //matches OauthController and all actions of controller by naming convention

    @Autowired(required = false)
    AuditLogger auditLogService
    @Autowired
    User currentUserBean

    boolean before() {
        auditLogService?.report("OAuth authentication", request,
                user: currentUserBean,
        )
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
