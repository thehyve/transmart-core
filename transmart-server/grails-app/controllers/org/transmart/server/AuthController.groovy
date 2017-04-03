package org.transmart.server

import grails.plugin.springsecurity.oauth2.SpringSecurityOauth2BaseService
import org.transmart.searchapp.AccessLog

class AuthController {

    /**
     * Dependency injection for the springSecurityService.
     */
    def springSecurityService
    SpringSecurityOauth2BaseService springSecurityOauth2BaseService

    def index = {
        new AccessLog(username: springSecurityService?.principal?.username, event: "Login",
                eventmessage: request.getHeader("user-agent"),
                accesstime: new Date()).save()
        render(view: '/auth/main.gsp', model: [user: springSecurityService?.principal?.username, redirectUri: "/login/forceAuth"])
    }

    def token = {
        def token = session[springSecurityOauth2BaseService.sessionKeyForAccessToken('google')]
        render(view: '/auth/token.gsp',
                model:[token: token])
    }

//    def onSuccess(String provider) {
//        if (!provider) {
//            log.warn "The Spring Security OAuth callback URL must include the 'provider' URL parameter"
//            throw new OAuth2Exception("The Spring Security OAuth callback URL must include the 'provider' URL parameter")
//        }
//        def sessionKey = springSecurityOauth2BaseService.sessionKeyForAccessToken(provider)
//        if (!session[sessionKey]) {
//            log.warn "No OAuth token in the session for provider '${provider}'"
//            throw new OAuth2Exception("Authentication error for provider '${provider}'")
//        }
//        // Create the relevant authentication token and attempt to log in.
//        OAuth2SpringToken oAuthToken = springSecurityOauth2BaseService.createAuthToken(provider, session[sessionKey])
//
//        if (oAuthToken.principal instanceof GrailsUser) {
//            authenticateAndRedirect(oAuthToken, getDefaultTargetUrl())
//        } else {
//            // This OAuth account hasn't been registered against an internal
//            // account yet. Give the oAuthID the opportunity to create a new
//            // internal account or link to an existing one.
//            session[SPRING_SECURITY_OAUTH_TOKEN] = oAuthToken
//
//            def redirectUrl = springSecurityOauth2BaseService.getAskToLinkOrCreateAccountUri()
//            if (!redirectUrl) {
//                log.warn "grails.plugin.springsecurity.oauth.registration.askToLinkOrCreateAccountUri configuration option must be set"
//                throw new OAuth2Exception('Internal error')
//            }
//            log.debug "Redirecting to askToLinkOrCreateAccountUri: ${redirectUrl}"
//            redirect(redirectUrl instanceof Map ? redirectUrl : [uri: redirectUrl])
//        }
//    }
//
//    OAuth2SpringToken createAuthToken(String providerName, OAuth2AccessToken scribeToken) {
//        OAuth2SpringToken oAuthToken = GoogleOAuth2Service.createSpringAuthToken(scribeToken)
//        def oAuthID = AccessToken.findByClientIdAndAuthentication(oAuthToken.providerName, oAuthToken.credentials as byte[])
//        if (oAuthID) {
//            updateOAuthToken(oAuthToken, oAuthID.user)
//        }
//        return oAuthToken
//    }
}
