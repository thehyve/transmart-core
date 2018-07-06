package org.transmartproject.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.transmartproject.core.users.UsersResource
import org.transmartproject.mock.MockUsersResource
import org.transmartproject.rest.TestResource
import org.transmartproject.test.TestService

@TestConfiguration
class RestApiTestConfiguration {

    @Bean
    @Primary
    UsersResource usersResource() {
        new MockUsersResource()
    }

    @Bean
    TestResource testResource() {
        new TestService()
    }

}
