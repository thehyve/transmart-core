package com.recomdata.security

import grails.plugin.springsecurity.oauthprovider.GormClientDetailsService
import grails.transaction.Transactional
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.ClientRegistrationException

class CustomGormClientDetailsService extends GormClientDetailsService {

    @Transactional("transactionManager_oauth2")
    ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        super.loadClientByClientId(clientId)
    }
}