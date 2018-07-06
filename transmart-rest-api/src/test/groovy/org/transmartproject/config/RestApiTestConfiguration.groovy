package org.transmartproject.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.transmartproject.db.test.H2Views
import org.transmartproject.mock.MockAuthContext
import org.transmartproject.rest.TestResource
import org.transmartproject.rest.user.AuthContext
import org.transmartproject.test.TestService

@TestConfiguration
class RestApiTestConfiguration {

    @Bean
    @Primary
    AuthContext authContext() {
        new MockAuthContext()
    }

    @Bean
    TestResource testResource() {
        new TestService()
    }

    @Bean
    H2Views h2Views() {
        new H2Views()
    }

}
