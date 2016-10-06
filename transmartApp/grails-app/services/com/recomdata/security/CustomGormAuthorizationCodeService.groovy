package com.recomdata.security

import grails.plugin.springsecurity.oauthprovider.GormAuthorizationCodeService
import grails.transaction.Transactional
import org.springframework.security.oauth2.provider.OAuth2Authentication

class CustomGormAuthorizationCodeService extends GormAuthorizationCodeService {

    @Transactional("transactionManager_oauth2")
    protected void store(String code, OAuth2Authentication authentication) {
        super.store(code, authentication)
    }

    @Transactional("transactionManager_oauth2")
    protected OAuth2Authentication remove(String code) {
        super.remove(code)
    }

}
