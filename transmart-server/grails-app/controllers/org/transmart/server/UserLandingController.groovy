package org.transmart.server

import com.github.scribejava.core.model.OAuth2AccessToken
import grails.plugin.springsecurity.oauth2.SpringSecurityOauth2BaseService
import grails.plugin.springsecurity.oauth2.exception.OAuth2Exception
import grails.plugin.springsecurity.oauth2.google.GoogleOAuth2Service
import grails.plugin.springsecurity.oauth2.token.OAuth2SpringToken
import grails.plugin.springsecurity.userdetails.GrailsUser
import org.springframework.web.servlet.support.RequestContextUtils
import org.transmart.oauth2.AccessToken
import org.transmart.searchapp.AccessLog

class UserLandingController {
    /**
     * Dependency injection for the springSecurityService.
     */
    def springSecurityService
    def messageSource
    SpringSecurityOauth2BaseService springSecurityOauth2BaseService

    private String getUserLandingPath() {
        grailsApplication.config.with {
            com.recomdata.defaults.landing ?: ui.tabs.browse.hide ? '/datasetExplorer' : '/RWG'
        }
    }

    def index = {
        new AccessLog(username: springSecurityService?.principal?.username, event: "Login",
                eventmessage: request.getHeader("user-agent"),
                accesstime: new Date()).save()
        render(view: '/userLanding/login.gsp', model:[user: springSecurityService?.principal?.username])
//        def skip_disclaimer = grailsApplication.config.com.recomdata?.skipdisclaimer ?: false;
//        if (skip_disclaimer) {
//            if (springSecurityService?.currentUser?.changePassword) {
//                flash.message = messageSource.getMessage('changePassword', new Objects[0], RequestContextUtils.getLocale(request))
//                redirect(controller: 'changeMyPassword')
//            } else {
//                redirect(uri: userLandingPath)
//            }
//        } else {
//            redirect(uri: '/userLanding/disclaimer.gsp')
//        }
    }
    def agree = {
        new AccessLog(username: springSecurityService?.principal?.username, event: "Disclaimer accepted",
                accesstime: new Date()).save()
        if (springSecurityService?.currentUser?.changePassword) {
            flash.message = messageSource.getMessage('changePassword', new Objects[0], RequestContextUtils.getLocale(request))
            redirect(controller: 'changeMyPassword')
        } else {
            redirect(uri: userLandingPath)
        }
    }

    def disagree = {
        new AccessLog(username: springSecurityService?.principal?.username, event: "Disclaimer not accepted",
                accesstime: new Date()).save()
        redirect(uri: '/logout')
    }

    def checkHeartBeat = {
        render(text: "OK")
    }

    def token = {
        def token = session[springSecurityOauth2BaseService.sessionKeyForAccessToken('google')]
        render(view: '/userLanding/token.gsp',
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
