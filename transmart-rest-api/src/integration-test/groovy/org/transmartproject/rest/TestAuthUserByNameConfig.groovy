package org.transmartproject.rest

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource
import org.transmartproject.rest.user.AuthContext

/**
 * Used to specify authorised user that exists in the database.
 */
@Configuration
class TestAuthUserByNameConfig {

    @Bean
    @Primary
    AuthContext authContext(UsersResource usersResource,
                            @Value("#{systemProperties['authUserName'] ?: 'user_-301'}") String authUserName) {
        new AuthContext() {
            @Override
            User getUser() {
                usersResource.getUserFromUsername(authUserName)
            }
        }
    }

}
