import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.oauth2.SpringSecurityOauth2BaseService
import org.apache.log4j.Logger
import org.springframework.beans.factory.config.MapFactoryBean
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.access.AccessDeniedHandlerImpl
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.transaction.interceptor.TransactionInterceptor
import org.springframework.web.util.IntrospectorCleanupListener
import org.transmart.oauth2.ActiveDirectoryLdapAuthenticationExtension
import org.transmart.oauth2.AuthUserDetailsService
import org.transmart.oauth2.BruteForceLoginLockService
import org.transmart.oauth2.LdapAuthUserDetailsMapper
import security.AuthSuccessEventListener
import security.BadCredentialsEventListener

def logger = Logger.getLogger('com.recomdata.conf.resources')

beans = {
    xmlns context: "http://www.springframework.org/schema/context"
    xmlns aop: "http://www.springframework.org/schema/aop"

    bruteForceLoginLockService(BruteForceLoginLockService) {
        allowedNumberOfAttempts = grailsApplication.config.bruteForceLoginLock.allowedNumberOfAttempts
        lockTimeInMinutes = grailsApplication.config.bruteForceLoginLock.lockTimeInMinutes
    }

    oAuth2BaseService(SpringSecurityOauth2BaseService)
    authSuccessEventListener(AuthSuccessEventListener) {
        bruteForceLoginLockService = ref('bruteForceLoginLockService')
    }

    badCredentialsEventListener(BadCredentialsEventListener) {
        bruteForceLoginLockService = ref('bruteForceLoginLockService')
    }
    sessionRegistry(SessionRegistryImpl)

    redirectStrategy(DefaultRedirectStrategy)
    accessDeniedHandler(AccessDeniedHandlerImpl) {
        errorPage = '/login'
    }
    failureHandler(SimpleUrlAuthenticationFailureHandler) {
        defaultFailureUrl = '/login'
    }


    transactionInterceptor(TransactionInterceptor) {
        transactionManagerBeanName = 'transactionManager'
        transactionAttributeSource = ref('transactionAttributeSource')
    }

    def transmartSecurity = grailsApplication.config.org.transmart.security
    if (SpringSecurityUtils.securityConfig.ldap.active) {
        ldapUserDetailsMapper(LdapAuthUserDetailsMapper) {
            springSecurityService = ref('springSecurityService')
            bruteForceLoginLockService = ref('bruteForceLoginLockService')
            // pattern for newly created user, can include <ID> for record id or <FEDERATED_ID> for external user name
            if (transmartSecurity.ldap.newUsernamePattern) {
                newUsernamePattern = transmartSecurity.ldap.newUsernamePattern
            }
            // comma separated list of new user authorities
            if (transmartSecurity.ldap.defaultAuthorities) {
                defaultAuthorities = transmartSecurity.ldap.defaultAuthorities
            }
            // if inheritPassword == false specified user will not be able to login without LDAP
            inheritPassword = transmartSecurity.ldap.inheritPassword
            // can be 'username' or 'federatedId'
            mappedUsernameProperty = transmartSecurity.ldap.mappedUsernameProperty
        }

        if (grailsApplication.config.org.transmart.security.ldap.ad.domain) {

            adExtension(ActiveDirectoryLdapAuthenticationExtension)

            aop {
                config("proxy-target-class": true) {
                    aspect(id: 'adExtensionService', ref: 'adExtension')
                }
            }

            ldapAuthProvider(ActiveDirectoryLdapAuthenticationProvider,
                    transmartSecurity.ldap.ad.domain,
                    SpringSecurityUtils.securityConfig.ldap.context.server
            ) {
                userDetailsContextMapper = ref('ldapUserDetailsMapper')
            }
        }
    }

    bruteForceLoginLockService(BruteForceLoginLockService) {
        allowedNumberOfAttempts = grailsApplication.config.bruteForceLoginLock.allowedNumberOfAttempts
        lockTimeInMinutes = grailsApplication.config.bruteForceLoginLock.lockTimeInMinutes
    }

    acghBedExporterRgbColorScheme(MapFactoryBean) {
        sourceMap = grailsApplication.config.dataExport.bed.acgh.rgbColorScheme
    }

    introspectorCleanupListener(IntrospectorCleanupListener)
    sessionRegistry(SessionRegistryImpl)
    redirectStrategy(DefaultRedirectStrategy)
    accessDeniedHandler(AccessDeniedHandlerImpl) {
        errorPage = '/login'
    }
    failureHandler(SimpleUrlAuthenticationFailureHandler) {
        defaultFailureUrl = '/login'
    }

    //overrides bean implementing GrailsUserDetailsService?
    userDetailsService(AuthUserDetailsService)

}
