package org.transmartproject.api.server.server

import groovy.util.logging.Slf4j
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * This filter denies access to users without any roles assigned to them.
 */
@Slf4j
class TransmartKeycloakAuthenticationProcessingFilter extends KeycloakAuthenticationProcessingFilter {

    TransmartKeycloakAuthenticationProcessingFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager)
        log.info("Enable filter that denies access to users without any roles assigned to them.")
    }

    @Override
    Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        Authentication authentication = super.attemptAuthentication(request, response)
        if (authentication && authentication.authorities.empty) {
            log.info("User ${authentication.principal} denied access. No authorities found.")
            throw new TransmartAuthenticationException("No authorities found.")
        }
        authentication
    }

}
