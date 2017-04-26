package org.transmartproject.app

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.provider.token.TokenStore
import org.transmart.oauth2.AccessToken


class UserApplicationsController {
    
    @Autowired
    private TokenStore tokenStore
    
    def springSecurityService
    
    def list = {
        def principal = springSecurityService.principal
        
        log.debug 'Fetching access tokens for ' + principal.username
        def tokens = AccessToken.withTransaction { AccessToken.findAll { username == principal.username }}
        def refreshTokenExpiration = [:]
        tokens.each {
            def t = tokenStore.readAccessToken(it.value)
            refreshTokenExpiration[it.id] = t.refreshToken?.expiration
        }
        render view: 'list', model: [tokens: tokens, refreshTokenExpiration: refreshTokenExpiration]
    }
    
    private removeTokens(token) {
        log.debug 'Removing access token ' + token.id
        if (token.refreshToken) {
            tokenStore.removeRefreshToken token.refreshToken
        }
        tokenStore.removeAccessToken token.value
    }
    
    def revoke = {
        def principal = springSecurityService.principal
        def token = AccessToken.withTransaction { AccessToken.find { id == params.id } }
        if (token.username == principal.username) {
            removeTokens(token)
        }
        flash.message = 'The access token has been revoked.'
        redirect (action: 'list')
    }
    
    def revokeAll = {
        def principal = springSecurityService.principal
        def tokens = AccessToken.withTransaction { AccessToken.findAll { username == principal.username } }
        tokens.each { token ->
            removeTokens(token)
        }
        flash.message = 'All access tokens have been revoked.'
        redirect (action: 'list')
    }
    
}