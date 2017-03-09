import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.transaction.interceptor.TransactionInterceptor
import org.transmart.oauth.ActiveDirectoryLdapAuthenticationExtension
import org.transmart.oauth.authentication.AuthUserDetailsService

import java.util.logging.Logger

def logger = Logger.getLogger('com.recomdata.conf.resources')

beans = {
    xmlns context: "http://www.springframework.org/schema/context"
    xmlns aop: "http://www.springframework.org/schema/aop"

    //oAuth2BaseService(SpringSecurityOauth2BaseService)


    //overrides bean implementing GrailsUserDetailsService?
    userDetailsService(AuthUserDetailsService)

    sessionRegistry(SessionRegistryImpl)

    redirectStrategy(DefaultRedirectStrategy)

    transactionInterceptor(TransactionInterceptor) {
        transactionManagerBeanName = 'transactionManager'
        transactionAttributeSource = ref('transactionAttributeSource')
    }

    def transmartSecurity = grailsApplication.config.org.transmart.security
    if (SpringSecurityUtils.securityConfig.ldap.active) {
        ldapUserDetailsMapper(com.recomdata.security.LdapAuthUserDetailsMapper) {
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

}
