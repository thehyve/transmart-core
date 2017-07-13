package org.transmart.oauth

import grails.plugin.springsecurity.oauth2.SpringSecurityOauth2BaseService
import org.transmart.searchapp.AccessLog

class Oauth2ProviderController {

    /**
     * Dependency injection for the springSecurityService.
     */
    def springSecurityService
    SpringSecurityOauth2BaseService springSecurityOauth2BaseService
    AuthService authService

//    def index = {
//        new AccessLog(username: springSecurityService?.principal?.username, event: "Login",
//                eventmessage: request.getHeader("user-agent"),
//                accesstime: new Date()).save()
//        render(view: '/auth/main.gsp', model: [user: springSecurityService?.principal?.username, redirectUri: "/login/forceAuth"])
//    }

    def googleToken = {
        def token = session[springSecurityOauth2BaseService.sessionKeyForAccessToken('google')]
        //authService.saveToken(token)
        render(view: '/auth/token.gsp',
                model: [token: token])
    }
}
