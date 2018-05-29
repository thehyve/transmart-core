package org.transmartproject.rest.misc

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.AccessDeniedException
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
    private UsersResource usersResourceService

    @Lazy
    User user = { ->
        def userPrincipal = SecurityContextHolder.context.authentication?.principal
        if (!userPrincipal) {
            throw new AccessDeniedException('No user principal has found.')
        }

        if (userPrincipal instanceof User) {
            log.debug('Principal is of the User type already. Returns the user.')
            return userPrincipal
        }

        String username
        if (userPrincipal instanceof String) {
            log.debug("Principal is a '${userPrincipal}' string. Interpreting it as username.")
            username = userPrincipal
        } else if (userPrincipal.hasProperty('username')) {
            log.debug("Principal has ${userPrincipal.class} type and has a username field.")
            username = userPrincipal.username
        } else {
            throw new AccessDeniedException("Unsuported principal type: ${userPrincipal.class}.")
        }
        usersResourceService.getUserFromUsername(username)
    }()

    @Override
    String toString() {
        "AuthContext(${user.toString()})"
    }

}
