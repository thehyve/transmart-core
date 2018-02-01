package transmart.oauth

import grails.core.GrailsApplication
import grails.plugin.springsecurity.SecurityFilterPosition
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugins.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.MapFactoryBean
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.access.AccessDeniedHandlerImpl
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy
import org.springframework.security.web.session.ConcurrentSessionFilter
import org.springframework.transaction.interceptor.TransactionInterceptor
import org.springframework.web.util.IntrospectorCleanupListener
import org.transmart.oauth.ActiveDirectoryLdapAuthenticationExtension
import org.transmart.oauth.AuthSuccessEventListener
import org.transmart.oauth.BadCredentialsEventListener
import org.transmart.oauth.CurrentUserBeanFactoryBean
import org.transmart.oauth.CurrentUserBeanProxyFactory
import org.transmart.oauth.LdapAuthUserDetailsMapper
import org.transmart.oauth.authentication.AuthUserDetailsService
import org.transmart.oauth.authentication.BruteForceLoginLockService
import org.transmartproject.core.users.User

class TransmartOauthGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.1.10 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Transmart Oauth"
    def author = "Transmart Foundation"
    def authorEmail = "admin@transmartproject.org"
    def description = '''\
OAuth plugin for Transmart
'''

    def license = "GPL3"

    def organization = [name: "The Hyve", url: "http://www.thehyve.nl"]

    //def developers = [
    //        [name: "Your name", email: "Your email"],
    //]

    def scm = [url: "https://github.com/thehyve/transmart-core"]
    def documentation = "https://github.com/thehyve/transmart-core"

    final static logger = LoggerFactory.getLogger(this)
    GrailsApplication grailsApplication

    Closure doWithSpring() {
        { ->
            xmlns context: "http://www.springframework.org/schema/context"
            xmlns aop: "http://www.springframework.org/schema/aop"

            sessionRegistry(SessionRegistryImpl)
            sessionAuthenticationStrategy(ConcurrentSessionControlAuthenticationStrategy, sessionRegistry) {
                if (grailsApplication.config.org.transmartproject.maxConcurrentUserSessions) {
                    maximumSessions = grailsApplication.config.org.transmartproject.maxConcurrentUserSessions
                } else {
                    maximumSessions = 10
                }
            }
            concurrentSessionFilter(ConcurrentSessionFilter) {
                sessionRegistry = sessionRegistry
                expiredUrl = '/login'
            }

            //overrides bean implementing GrailsUserDetailsService?
            userDetailsService(AuthUserDetailsService)


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

            if (grailsApplication.config.org.transmart.security.samlEnabled) {
                importBeans('classpath:/spring/spring-security-saml.xml')
                // Provider of default SAML Context. Moved to groovy to allow choose implementation
                if (grailsApplication.config.org.transmart.security.saml.lb.serverName) {
                    contextProvider(org.springframework.security.saml.context.SAMLContextProviderLB) {
                        scheme = grailsApplication.config.org.transmart.security.saml.lb.scheme
                        serverName = grailsApplication.config.org.transmart.security.saml.lb.serverName
                        serverPort = grailsApplication.config.org.transmart.security.saml.lb.serverPort
                        includeServerPortInRequestURL = grailsApplication.config.org.transmart.security.saml.lb.includeServerPortInRequestURL
                        contextPath = grailsApplication.config.org.transmart.security.saml.lb.contextPath
                    }
                } else {
                    contextProvider(org.springframework.security.saml.context.SAMLContextProviderImpl)
                }
            }

            bruteForceLoginLockService = ref('bruteForceLoginLockService')
            bruteForceLoginLockService(BruteForceLoginLockService) {
                allowedNumberOfAttempts = grailsApplication.config.bruteForceLoginLock.allowedNumberOfAttempts
                lockTimeInMinutes = grailsApplication.config.bruteForceLoginLock.lockTimeInMinutes
            }

            authSuccessEventListener(AuthSuccessEventListener) {
                bruteForceLoginLockService = ref('bruteForceLoginLockService')
            }

            badCredentialsEventListener(BadCredentialsEventListener) {
                bruteForceLoginLockService = ref('bruteForceLoginLockService')
            }

            acghBedExporterRgbColorScheme(MapFactoryBean) {
                sourceMap = grailsApplication.config.dataExport.bed.acgh.rgbColorScheme
            }

            introspectorCleanupListener(IntrospectorCleanupListener)

            //overrides bean implementing GrailsUserDetailsService
            userDetailsService(AuthUserDetailsService)

            currentUserBean(CurrentUserBeanProxyFactory)
            "${CurrentUserBeanProxyFactory.SUB_BEAN_REQUEST}"(CurrentUserBeanFactoryBean) { bean ->
                bean.scope = 'request'
            }
            "${CurrentUserBeanProxyFactory.SUB_BEAN_QUARTZ}"(User) { bean ->
                // Spring never actually creates this bean
                bean.scope = 'quartz'
            }
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {

        if (grailsApplication.config.org.transmart.security.samlEnabled) {
            SpringSecurityUtils.clientRegisterFilter(
                    'metadataGeneratorFilter', SecurityFilterPosition.FIRST)
            SpringSecurityUtils.clientRegisterFilter(
                    'samlFilter', SecurityFilterPosition.BASIC_AUTH_FILTER)
        }
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
