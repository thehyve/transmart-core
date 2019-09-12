package org.transmartproject.api.server.user

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount
import org.keycloak.representations.AccessToken
import org.keycloak.representations.idm.ClientMappingsRepresentation
import org.keycloak.representations.idm.MappingsRepresentation
import org.keycloak.representations.idm.RoleRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.*

import java.security.Principal

import static org.transmartproject.api.server.user.AccessLevels.*

@Component
@Primary
@Slf4j
@CompileStatic
class KeycloakUserResourceService implements UsersResource {

    @Autowired
    LegacyAuthorisationChecks authorisationChecks

    @Autowired
    @Qualifier("offlineTokenBasedRestTemplate")
    RestOperations offlineTokenBasedRestTemplate

    @Value('${keycloak.realm}')
    String realm

    @Value('${keycloak.resource}')
    String clientId

    @Value('${keycloak.auth-server-url}')
    String keycloakServerUrl

    @Override
    User getUserFromUsername(String username) throws NoSuchResourceException {
        User user = getUsers()?.find { it.username == username }
        if (!user) {
            throw new NoSuchResourceException("No user with '${username}' username found.")
        }
        user
    }

    static ParameterizedTypeReference<List<UserRepresentation>> userListRef =
            new ParameterizedTypeReference<List<UserRepresentation>>() {}

    @Override
    List<User> getUsers() {
        ResponseEntity<List<UserRepresentation>> response = offlineTokenBasedRestTemplate
                .exchange("${keycloakServerUrl}/admin/realms/${realm}/users".toString(),
                        HttpMethod.GET, null, userListRef)
        response.body.collect { UserRepresentation keycloakUser ->
            Set<String> roles = getRolesForUser(keycloakUser.id)
            createUser(keycloakUser, roles)
        }
    }

    @Override
    List<User> getUsersWithEmailSpecified() {
        getUsers()?.findAll { it.email }
    }

    @Override
    User getUserFromPrincipal(Principal principal) {
        assert principal instanceof Authentication
        if (!principal.authenticated) {
            throw new IllegalArgumentException("${principal.name} principal has authenticated flag set to false.")
        }

        final String username = principal.name
        List<String> authorities = principal.authorities.collect { GrantedAuthority ga -> ga.authority }
        authorities.remove(ROLE_PUBLIC)
        final boolean admin = authorities.remove(ROLE_ADMIN)
        Map<String, PatientDataAccessLevel> studyToAccLvl =
                buildStudyToPatientDataAccessLevel(authorities)

        String realName = null
        String email = null
        if (principal.details instanceof SimpleKeycloakAccount) {
            def context = ((SimpleKeycloakAccount) principal.details).keycloakSecurityContext
            if (context?.token) {
                AccessToken token = context.token
                realName = token.name
                email = token.email
            } else {
                log.debug("No token in the security context. Giving up on getting email and name.")
            }
        } else {
            log.debug("The details field of unexpected type: ${principal.details?.class}. Giving up on getting email and name.")
        }

        new SimpleUser(username, realName, email, admin, studyToAccLvl)
    }

    private Set<String> getRolesForUser(String userId) {
        ResponseEntity<MappingsRepresentation> result = offlineTokenBasedRestTemplate.getForEntity(
                "$keycloakServerUrl/admin/realms/$realm/users/$userId/role-mappings".toString(),
                MappingsRepresentation.class)

        Map<String, ClientMappingsRepresentation> rolesPerClient = result.body.clientMappings
        Set<String> roles = []
        ClientMappingsRepresentation clientMappings = rolesPerClient.get(clientId)
        if (clientMappings) {
            for (RoleRepresentation roleRepresentation: clientMappings.mappings) {
                roles.add(roleRepresentation.name)
            }
        } else {
            log.debug("No client role mappings for $clientId client were found.")
        }
        log.debug("${userId} user has following roles on ${clientId} client: ${roles}.")

        roles
    }

    private static User createUser(UserRepresentation keycloakUser, Set<String> roles) {
        roles.remove(ROLE_PUBLIC)
        final boolean admin = roles.remove(ROLE_ADMIN)
        Map<String, PatientDataAccessLevel> studyToPatientDataAccessLevel = buildStudyToPatientDataAccessLevel(roles)
        new SimpleUser(keycloakUser.id,
                "$keycloakUser.firstName $keycloakUser.lastName",
                keycloakUser.email,
                admin,
                studyToPatientDataAccessLevel)
    }
}
