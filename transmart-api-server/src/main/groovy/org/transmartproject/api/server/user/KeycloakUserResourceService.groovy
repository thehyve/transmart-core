package org.transmartproject.api.server.user

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.stereotype.Component
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.users.*

import java.security.Principal
import java.text.ParseException

@Component
@Primary
@Slf4j
class KeycloakUserResourceService implements UsersResource {

    @Autowired
    LegacyAuthorisationChecks authorisationChecks

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
        assert principal instanceof Authentication

        final String username = principal.name
        List<String> authorities = principal.authorities*.authority
        final boolean admin = authorities.remove('ROLE_ADMIN')
        Map<String, PatientDataAccessLevel> studyToAccLvl =
                buildStudyToPatientDataAccessLevel(authorities)

        final String realName
        final String email
        if (principal instanceof OAuth2Authentication
                && principal.userAuthentication
                && principal.userAuthentication.details instanceof Map) {
            Map details = principal.userAuthentication.details
            realName = details.name
            email = details.email
        } else {
            log.warn("Unexpected or incomplete authentication object ${principal}. Hence email and name can't be fetched.")
            realName = null
            email = null
        }

        new SimpleUser(username, realName, email, admin, studyToAccLvl)
    }

    private static Map<String, PatientDataAccessLevel> buildStudyToPatientDataAccessLevel(final Collection<String> roles) {
        Map<String, PatientDataAccessLevel> result = [:]
        for (String studyTokenToAccLvl : roles) {
            try {
                Tuple2<String, PatientDataAccessLevel> studyTokenToAccessLevel = parseStudyTokenToAccessLevel(studyTokenToAccLvl)
                String studyToken = studyTokenToAccessLevel.first
                PatientDataAccessLevel accLvl = studyTokenToAccessLevel.second
                if (result.containsKey(studyToken)) {
                    PatientDataAccessLevel memorisedAccLvl = result.get(studyToken)
                    if (accLvl > memorisedAccLvl) {
                        log.debug("Use ${accLvl} access level instead of ${memorisedAccLvl} on ${studyToken} study.")
                        result.put(studyToken, accLvl)
                    } else {
                        log.debug("Keep ${memorisedAccLvl} access level and ignore ${accLvl} on ${studyToken} study.")
                    }
                } else {
                    result.put(studyToken, accLvl)
                    log.debug("Found ${accLvl} access level on ${studyToken} study.")
                }
            } catch (Exception e) {
                log.error("Can't parse permission '${studyTokenToAccLvl}'.", e)
            }
        }
        Collections.unmodifiableMap(result)
    }

    private static Tuple2<String, PatientDataAccessLevel> parseStudyTokenToAccessLevel(String studyTokenToAccLvl) {
        String[] studyTokenToAccLvlSplit = studyTokenToAccLvl.split('\\|')
        if (studyTokenToAccLvlSplit.length != 2) {
            throw new ParseException("Can't parse permission '${studyTokenToAccLvl}'.", 0)
        }

        String studyToken = studyTokenToAccLvlSplit[0]
        if (!studyToken) {
            throw new IllegalArgumentException("Emtpy study: '${studyTokenToAccLvl}'.")
        }
        String accessLevel = studyTokenToAccLvlSplit[1]
        new Tuple2(studyToken, PatientDataAccessLevel.valueOf(accessLevel))
    }
}
