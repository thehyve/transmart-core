package transmartapp

import com.google.common.collect.ImmutableMap
import com.recomdata.extensions.ExtensionsRegistry
import grails.plugins.*
import org.grails.spring.DefaultBeanConfiguration
import org.springframework.beans.factory.config.CustomScopeConfigurer
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.access.AccessDeniedHandlerImpl
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.authentication.session.ConcurrentSessionControlStrategy
import org.springframework.security.web.session.ConcurrentSessionFilter
import org.springframework.transaction.interceptor.TransactionInterceptor
import org.springframework.web.util.IntrospectorCleanupListener
import org.transmart.oauth.CurrentUserBeanFactoryBean
import org.transmart.oauth.CurrentUserBeanProxyFactory
import org.transmart.authorization.QueriesResourceAuthorizationDecorator
import org.transmart.marshallers.MarshallerRegistrarService
import org.transmart.oauth.authentication.AuthUserDetailsService
import org.transmart.spring.QuartzSpringScope
import org.transmartproject.core.users.User
import org.transmartproject.export.HighDimExporter

class TransmartAppGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.2.3 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Transmart App" // Headline display name of the plugin
    def author = "Your name"
    def authorEmail = ""
    def description = '''\
Brief summary/description of the plugin.
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/transmart-app"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    Closure doWithSpring() {
        { ->
            sessionRegistry(SessionRegistryImpl)
            sessionAuthenticationStrategy(ConcurrentSessionControlStrategy, sessionRegistry) {
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
            xmlns context: "http://www.springframework.org/schema/context"
            xmlns aop: "http://www.springframework.org/schema/aop"

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

            //overrides bean implementing GrailsUserDetailsService?
            userDetailsService(AuthUserDetailsService)

            /* core-api authorization wrapped beans */
            queriesResourceAuthorizationDecorator(QueriesResourceAuthorizationDecorator) {
                DefaultBeanConfiguration bean ->
                    bean.beanDefinition.autowireCandidate = false
            }

            quartzSpringScope(QuartzSpringScope)
            quartzScopeConfigurer(CustomScopeConfigurer) {
                scopes = ImmutableMap.of('quartz', ref('quartzSpringScope'))
            }

            legacyQueryResultAccessCheckRequestCache(
                    QueriesResourceAuthorizationDecorator.LegacyQueryResultAccessCheckRequestCache) { bean ->
                bean.scope = 'request'
            }

            context.'component-scan'('base-package': 'org.transmartproject.export') {
                context.'include-filter'(
                        type: 'assignable',
                        expression: HighDimExporter.canonicalName)
            }

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

            marshallerRegistrarService(MarshallerRegistrarService)

            acghBedExporterRgbColorScheme(org.springframework.beans.factory.config.MapFactoryBean) {
                sourceMap = grailsApplication.config.dataExport.bed.acgh.rgbColorScheme
            }

            transmartExtensionsRegistry(ExtensionsRegistry) {
            }

            introspectorCleanupListener(IntrospectorCleanupListener)

        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
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
