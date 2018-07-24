package org.transmartproject.rest

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.grails.orm.hibernate5.support.GrailsOpenSessionInViewInterceptor
import org.hibernate.SessionFactory
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.web.context.request.WebRequestInterceptor
import org.transmartproject.core.log.AccessLogEntryResource
import org.transmartproject.db.test.H2Views
import org.transmartproject.mock.MockAccessLogEntryResource
import org.transmartproject.mock.MockAuthContext
import org.transmartproject.rest.user.AuthContext
import org.transmartproject.test.TestService

import javax.validation.Validation
import javax.validation.Validator

/**
 * Test application with injected test services. Such as data and current user.
 */
class TestApplication extends GrailsAutoConfiguration {

    @Bean
    TestResource testResource(SessionFactory sessionFactory) {
        new TestService(sessionFactory: sessionFactory)
    }

    @Bean
    @Primary
    AuthContext authContext() {
        new MockAuthContext()
    }

    @Bean
    @Primary
    AccessLogEntryResource mockAccessLogEntryResource() {
        new MockAccessLogEntryResource()
    }

    /**
     * Build h2 views after db schema has been created.
     * @return
     */
    @Bean
    H2Views h2Views(SessionFactory sessionFactory) {
        new H2Views(sessionFactory: sessionFactory)
    }

    @Bean
    EmbeddedServletContainerFactory containerFactory() {
        new TomcatEmbeddedServletContainerFactory(0)
    }

    /**
     * To make calls without @Transactional to work.
     * @param sessionFactory
     * @return
     */
    @Bean
    WebRequestInterceptor[] webRequestInterceptors(SessionFactory sessionFactory) {
        [
                new GrailsOpenSessionInViewInterceptor(sessionFactory: sessionFactory)
        ] as WebRequestInterceptor[]
    }

    @Bean
    Validator validator() {
        Validation.buildDefaultValidatorFactory().getValidator()
    }

    static void main(String[] args) {
        GrailsApp.run(TestApplication, args)
    }
}
