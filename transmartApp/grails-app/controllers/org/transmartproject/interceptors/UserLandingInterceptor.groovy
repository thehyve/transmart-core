package org.transmartproject.interceptors


class UserLandingInterceptor {
    def auditLogService
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
