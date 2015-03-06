package org.transmartproject.rest.misc

import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.util.logging.Log4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.AccessDeniedException
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.ProtectedResource
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource

/**
 * Spring request-scoped bean that makes it easy to fetch the logged in user.
 */
@Component
@Scope(value = 'request', proxyMode = ScopedProxyMode.TARGET_CLASS)
@Log4j
class CurrentUser implements User {

    @Autowired
    private UsersResource usersResourceService

    @Autowired(required = false)
    SpringSecurityService springSecurityService

    @Lazy
    private User delegate = { ->
        if (springSecurityService == null ||
                !SpringSecurityUtils.securityConfig.active) {
            log.warn "Spring security service not available or inactive, " +
                    "returning dummy user administrator"
            return new DummyAdministrator()
        }

        if (!springSecurityService.isLoggedIn()) {
            log.warn "User is not logged in; throwing"
            throw new AccessDeniedException('User is not logged in')
        }

        def username = springSecurityService.principal.username

        usersResourceService.getUserFromUsername(username)
    }()

    @Override
    Long getId() {
        delegate.id
    }

    @Override
    String getUsername() {
        delegate.username
    }

    @Override
    String getRealName() {
        delegate.realName
    }

    @Override
    boolean canPerform(ProtectedOperation operation, ProtectedResource protectedResource) {
        delegate.canPerform(operation, protectedResource)
    }

    void checkAccess(ProtectedOperation operation, ProtectedResource resource) {
        if (!canPerform(operation, resource)) {
            throw new AccessDeniedException("Denied user $this permission " +
                    "to effect operation $operation on resource $resource")
        }
    }

    @Override
    String toString() {
        "CurrentUser(${delegate.toString()})"
    }

    static class DummyAdministrator implements User {

        /* These correspond to the properties of the default transmart
         * administrator user */
        final Long id = 1
        final String username = 'admin'
        final String realName = 'Sys Admin'

        @Override
        boolean canPerform(ProtectedOperation operation, ProtectedResource protectedResource) {
            true
        }
    }
}
