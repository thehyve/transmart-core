package org.transmartproject.api.server.user.keycloak

import groovy.transform.CompileStatic
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor

/**
 *  Parse sub (unique id) from the jwt token as principal (username).
 *  The reason to use `sub` instead of `preferred_username` is that latter does not guarantees uniqueness across system.
 *  See http://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
 *  Used by {@link org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices}
 *  By default {@link org.springframework.boot.autoconfigure.security.oauth2.resource.FixedPrincipalExtractor} that picks `name`.
 *  Which is not good username candidate anyway.
 */
@CompileStatic
class SubPrincipalExtractor implements PrincipalExtractor {
    @Override
    Object extractPrincipal(Map<String, Object> map) {
        map.sub
    }
}
