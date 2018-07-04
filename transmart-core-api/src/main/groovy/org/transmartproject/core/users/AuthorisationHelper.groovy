package org.transmartproject.core.users

import groovy.transform.CompileStatic

import java.util.stream.Collectors

@CompileStatic
class AuthorisationHelper {

    static final String PUBLIC_SOT = 'EXP:PUBLIC'
    static final String LEGACY_PUBLIC_SOT = 'PUBLIC'
    static final Set<String> PUBLIC_TOKENS = [LEGACY_PUBLIC_SOT, PUBLIC_SOT] as Set

    /**
     * Retrieves study ids for the studies to which the user has at least the required access level.
     * @param user
     * @param requiredAccessLevel
     * @return the set of study ids.
     */
    static Set<String> getStudyTokensForUser(User user, PatientDataAccessLevel requiredAccessLevel) {
        user.studyToPatientDataAccessLevel.entrySet().stream()
                .filter({ Map.Entry<String, PatientDataAccessLevel> entry ->
                    entry.value >= requiredAccessLevel
                })
                .map({ Map.Entry<String, PatientDataAccessLevel> entry -> entry.key })
                .collect(Collectors.toSet()) + PUBLIC_TOKENS
    }

    /**
     * Checks whether user has at least the required access level for the given study token
     *
     * @param user
     * @param requiredAccessLevel required access level
     * @param token the study token
     * @return true iff the user has at least the required access level for the study.
     */
    static boolean hasAtLeastAccessLevel(User user, PatientDataAccessLevel requiredAccessLevel, String token) {
        if (!token) {
            throw new IllegalArgumentException('Token is null.')
        }

        if (user.admin) {
            return true
        }

        if (token in PUBLIC_TOKENS) {
            return true
        }

        requiredAccessLevel <= user.studyToPatientDataAccessLevel[token]
    }

}
