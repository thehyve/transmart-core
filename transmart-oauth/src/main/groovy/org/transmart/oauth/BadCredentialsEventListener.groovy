package org.transmart.oauth

import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent
import org.springframework.security.authentication.event.AuthenticationFailureServiceExceptionEvent
import org.transmart.oauth.authentication.BruteForceLoginLockService

class BadCredentialsEventListener implements ApplicationListener<AbstractAuthenticationFailureEvent> {

    BruteForceLoginLockService bruteForceLoginLockService

    void onApplicationEvent(AbstractAuthenticationFailureEvent event) {
        //We have to check for AuthenticationFailureServiceExceptionEvent because of bug in current version of OAuth provider
        //see https://github.com/spring-projects/spring-security-oauth/pull/383
        if ([AuthenticationFailureBadCredentialsEvent, AuthenticationFailureServiceExceptionEvent].any {
            it.isAssignableFrom(event.class)
        }) {
            bruteForceLoginLockService.failLogin(event.authentication.name)
        }
    }

}
