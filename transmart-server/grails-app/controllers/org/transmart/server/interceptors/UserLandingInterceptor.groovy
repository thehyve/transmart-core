package org.transmart.server.interceptors

import org.transmartproject.core.users.User


class UserLandingInterceptor {
    def auditLogService
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
