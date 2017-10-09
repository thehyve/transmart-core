package org.transmart.oauth

import grails.core.GrailsApplication
import org.springframework.security.web.context.SecurityContextPersistenceFilter

class BootStrap {

    SecurityContextPersistenceFilter securityContextPersistenceFilter

    GrailsApplication grailsApplication

    OAuth2SyncService OAuth2SyncService

    def init = {
        securityContextPersistenceFilter.forceEagerSessionCreation = true

        if ('clientCredentialsAuthenticationProvider' in
                grailsApplication.config.grails.plugin.springsecurity.providerNames) {
            OAuth2SyncService.syncOAuth2Clients()
        }

    }

}
