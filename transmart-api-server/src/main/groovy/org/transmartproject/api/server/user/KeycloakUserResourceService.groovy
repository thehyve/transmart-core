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
        def details = authentication.details
        assert details instanceof Map

        final String username = details.sub
        assert username

        final String realName = details.name
        final String email = details.email
        Collection<String> roles = details.roles
        final boolean admin = roles.remove('ROLE_ADMIN')
        ImmutableMultimap<String, ProtectedOperation> accessStudyTokenToOperations =
                buildStudyTokenToOperationsMultimap(roles, username)

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
