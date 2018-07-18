package org.transmartproject.rest.user

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource

/**
 * Spring security based authentication.
 * Has to live in the request or session scope to prevent sharing state (potentially) between user.
 */
@Component
@Scope(value = 'request', proxyMode = ScopedProxyMode.TARGET_CLASS)
class SpringSecurityAuthContext implements AuthContext {

    @Autowired
    private UsersResource usersResource

    @Override
    User getUser() {
        usersResource.getUserFromPrincipal(getAuthentication())
    }

    @Override
    String toString() {
        "SpringSecurityAuthContext(${getAuthentication().name})"
    }

    protected Authentication getAuthentication() {
        SecurityContextHolder.context.authentication
    }

}
