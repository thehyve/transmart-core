package org.transmartproject.app

import com.google.common.collect.ImmutableMap
import com.recomdata.extensions.ExtensionsRegistry
import grails.core.GrailsApplication
import grails.plugin.springsecurity.SecurityFilterPosition
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugins.Plugin
import org.grails.spring.DefaultBeanConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.CustomScopeConfigurer
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.authentication.session.ConcurrentSessionControlStrategy
import org.springframework.security.web.session.ConcurrentSessionFilter
import org.springframework.transaction.interceptor.TransactionInterceptor
import org.springframework.web.util.IntrospectorCleanupListener
import org.transmart.authorization.QueriesResourceAuthorizationDecorator
import org.transmart.marshallers.MarshallerRegistrarService
import org.transmart.oauth.authentication.AuthUserDetailsService
import org.transmart.spring.QuartzSpringScope
import org.transmartproject.export.HighDimExporter

class TransmartAppGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.1.10 > *"

    def title = "Transmart App"
    def author = "Transmart Foundation"
    def authorEmail = "admin@transmartproject.org"
    def description = '''\
Legacy Grails application for Transmart
'''

    def organization = [name: "The Hyve", url: "http://www.thehyve.nl/"]

    def developers = [
            // many
    ]

    def scm = [url: "https://github.com/thehyve/transmart-core"]
    def documentation = "https://github.com/thehyve/transmart-core"

    def profiles = ['web']

    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]
    // to override login/auth.gsp and other GSP pages from Spring Security core
    List loadAfter = ['springSecurityCore']

    final static logger = LoggerFactory.getLogger(this)
    GrailsApplication grailsApplication

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

        SpringSecurityUtils.clientRegisterFilter('concurrentSessionFilter', SecurityFilterPosition.CONCURRENT_SESSION_FILTER)

        // force marshaller registrar initialization
        grailsApplication.mainContext.getBean 'marshallerRegistrarService'
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
