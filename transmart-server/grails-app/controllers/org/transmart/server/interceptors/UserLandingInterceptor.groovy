package org.transmart.server.interceptors

import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.audit.AuditLogger
import org.transmartproject.core.users.User


class UserLandingInterceptor {

    @Autowired(required = false)
    AuditLogger auditLogService
    @Autowired
    User currentUserBean

    UserLandingInterceptor(){
        match(controller: 'userLanding').excludes(action: 'checkHeartBeat')
    }

    boolean before() {
        auditLogService.report("User Access", request,
                user: currentUserBean,
        )
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
