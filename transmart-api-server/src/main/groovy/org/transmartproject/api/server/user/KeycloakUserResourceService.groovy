package org.transmartproject.api.server.user

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.AuthorisationChecks
import org.transmartproject.core.users.ProtectedOperation
import org.transmartproject.core.users.ProtectedResource
import org.transmartproject.core.users.User
import org.transmartproject.core.users.UsersResource

import java.security.Principal
import java.text.ParseException

@Component
@Primary
@Slf4j
class KeycloakUserResourceService implements UsersResource {

    @Autowired
    AuthorisationChecks authorisationChecks

    @Override
    User getUserFromUsername(String username) throws NoSuchResourceException {
        throw new UnsupportedOperationException()
    }

    @Override
    List<User> getUsers() {
        throw new UnsupportedOperationException()
    }

    @Override
    List<User> getUsersWithEmailSpecified() {
        throw new UnsupportedOperationException()
    }

    @Override
    User getUserFromPrincipal(Principal principal) {
        assert principal instanceof OAuth2Authentication
        def authentication = principal.userAuthentication
        assert authentication: 'User is not authenticated.'

        final String username = principal.name
        List<String> authorities = principal.authorities*.authority
        final boolean admin = authorities.remove('ROLE_ADMIN')
        Multimap<String, ProtectedOperation> accessStudyTokenToOperations =
                buildStudyTokenToOperationsMultimap(authorities, username)

        def details = authentication.details
        final String realName
        final String email
        if (details instanceof Map) {
            realName = details.name
            email = details.email
        } else {
            log.warn("Unexpected user details object for ${username} user. Expected map but was ${details}. Hence email and name can't be parsed.")
            realName = null
            email = null
        }

        createUser(username, realName, email, admin, accessStudyTokenToOperations)
    }

    private User createUser(final String username,
                            final String realName,
                            final String email,
                            final boolean admin,
                            final Multimap<String, ProtectedOperation> accessStudyTokenToOperations) {
        new User() {
            @Override
            Long getId() {
                throw new UnsupportedOperationException()
            }

            @Override
            String getUsername() {
                username
            }

            @Override
            String getRealName() {
                realName
            }

            @Override
            String getEmail() {
                email
            }

            @Override
            boolean canPerform(ProtectedOperation operation, ProtectedResource protectedResource) {
                authorisationChecks.canPerform(this, operation, protectedResource)
            }

            @Override
            boolean isAdmin() {
                admin
            }

            @Override
            Multimap<String, ProtectedOperation> getAccessStudyTokenToOperations() {
                accessStudyTokenToOperations
            }
        }
    }

    private static Multimap<String, ProtectedOperation> buildStudyTokenToOperationsMultimap(Collection<String> roles, String username) {
        def multimapBuilder = ImmutableMultimap.<String, ProtectedOperation> builder()
        roles.each { String opToStudyToken ->
            try {
                Tuple2<String, ProtectedOperation> studyTokenToOperation = parseStudyTokenToOperation(opToStudyToken)
                multimapBuilder.put(studyTokenToOperation.first, studyTokenToOperation.second)
            } catch (Exception e) {
                log.error("Can't parse permission '${opToStudyToken}' for user '${username}'.", e)
            }
        }
        multimapBuilder.build()
    }

    private static Tuple2<String, ProtectedOperation> parseStudyTokenToOperation(String opToStudyToken) {
        String[] opToStudyTokenSplit = opToStudyToken.split('\\|')
        if (opToStudyTokenSplit.length != 2) {
            throw new ParseException("Can't parse permission '${opToStudyToken}'.", 0)
        }

        String operation = opToStudyTokenSplit[0]
        String studyToken = opToStudyTokenSplit[1]
        new Tuple2(studyToken, ProtectedOperation.WellKnownOperations.valueOf(operation))
    }
}
