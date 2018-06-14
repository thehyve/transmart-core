package org.transmart.server.interceptors

import groovy.transform.CompileStatic
import org.transmartproject.core.log.AccessLogEntryResource
import org.transmartproject.rest.user.AuthContext

@CompileStatic
class UserLandingInterceptor {

    AccessLogEntryResource accessLogService
    AuthContext authContext

    UserLandingInterceptor(){
        match(controller: 'userLanding').excludes(action: 'checkHeartBeat')
    }

    boolean before() {
        def eventMessage = ''
        if (actionName == 'index'){
            eventMessage = "Login Attempt"
        } else if (actionName == 'agree') {
            eventMessage = "Login Successful"
        } else if (actionName == 'disagree') {
            eventMessage = "Login Failed"
        }

        accessLogService.report(
                authContext.user,
                "User Access",
                eventMessage: eventMessage,
                accessTime: new Date())
        true
    }

    boolean after() { true }

}
