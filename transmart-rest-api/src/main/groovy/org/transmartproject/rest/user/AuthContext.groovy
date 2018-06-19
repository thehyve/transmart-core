package org.transmartproject.rest.user

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource

/**
 * Spring request-scoped bean that makes it easy to fetch the logged in user.
 */
@Component
@Scope(value = 'request', proxyMode = ScopedProxyMode.TARGET_CLASS)
@Slf4j
class AuthContext {

    @Autowired
    private UsersResource usersResource

    @Lazy
    User user = { ->
        usersResource.getUserFromPrincipal(SecurityContextHolder.context.authentication)
    }()

    @Override
    String toString() {
        "AuthContext(${user.toString()})"
    }

}
