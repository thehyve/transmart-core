package org.transmartproject.app

import grails.core.GrailsApplication
import grails.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.provider.token.TokenStore
import org.transmart.oauth2.Client

@Transactional
class OauthAdminController {
    
    @Autowired
    private TokenStore tokenStore
    def springSecurityService
    
    GrailsApplication grailsApplication

    def index = {
        redirect action: 'list', params: params
    }
    
    def list = {
            // fetchMode: eager added to avoid org.hibernate.LazyInitializationException for Client objects,
            // causing error, when processing GroovyPageView: "failed to lazily initialize a collection of role xxx."
            def clients = Client.findAll([fetch: [redirectUris: 'eager', authorizedGrantTypes: 'eager']])

            def configClients = grailsApplication.config.grails.plugin.springsecurity.oauthProvider.clients
            Set configClientIds = configClients*.clientId
            log.info configClientIds.toString()

            render view: 'list', model: [clients: clients, configClientIds: configClientIds]
    }
    
    def create = {
        def client = new Client()
        client.redirectUris = []
        client.clientId = ''
        client.authorizedGrantTypes = []
        render view: 'edit', model: [client: client]
    }
    
    private findClient(id) {
        // fetchMode: eager added to avoid org.hibernate.LazyInitializationException for Client objects,
        // causing error, when processing GroovyPageView: "failed to lazily initialize a collection of role xxx."
        def client = Client.findById(params.id, [fetch: [redirectUris: 'eager', authorizedGrantTypes: 'eager']])
        if (!client) {
            flash.message = "Client application not found with id $params.id"
            redirect action: 'list'
            return
        }
        client
    }
    
    def edit = {
        def client = findClient(params.id)
        if (!client) {
            return
        }
        render view: 'edit', model: [client: client]
    }
    
    def view = {
        def client = findClient(params.id)
        if (!client) {
            return
        }
        render view: 'view', model: [client: client]
    }
    
    def save = {
        log.debug 'Save client. Data: ' + params
        def client
        if (params.id) {
            client = findClient(params.id)
        } else {
            client = new Client()
        }
        if (!client) {
            return
        }
        
        client.scopes = ["_dummy_"]
        
        params.clientSecret = params.clientSecret?.trim()
        if (!params.clientSecret) {
            params.remove('clientSecret')
        }
        
        client.properties = params
        
        def redirectUris = []
        client.redirectUris.each {
            def uri = it.trim()
            if (uri) {
                redirectUris << uri
            }
        }
        client.redirectUris = redirectUris
        
        if (client.validate() && client.save(flush: true)) {
            log.debug 'client saved: ' + client.id
            redirect (action: 'view', id: client.id)
        } else {
            log.error 'saving client failed'
            render view: 'edit', model: [client: client]
        }
    }
    
    def delete = {
        def client = findClient(params.id)
        if (!client)  {
            return
        }
        
        log.debug 'Removing client with client ID ${params.id}'
        
        // Remove associated tokens from tokenStore
        def tokens = tokenStore.findTokensByClientId(params.id)
        tokens.each { token ->
            log.debug 'Removing refresh token ${token.refreshToken} and access token ${token.value}'
            if (token.refreshToken) {
                tokenStore.removeRefreshToken(token.refreshToken)
            }
            tokenStore.removeAccessToken(token)
        }
        
        def clientId = client.clientId
        client.delete()
        flash.message = "Client application $clientId deleted."
        redirect action: 'list'
    }
}