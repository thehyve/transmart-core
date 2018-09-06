package org.transmartproject.core.users

import groovy.transform.CompileStatic

import java.util.function.Function
import java.util.stream.Collectors

import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS
import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS_WITH_THRESHOLD

@CompileStatic
class AuthorisationHelper {

    static final String PUBLIC_SOT = 'EXP:PUBLIC'
    static final String LEGACY_PUBLIC_SOT = 'PUBLIC'
    static final Set<String> PUBLIC_TOKENS = [LEGACY_PUBLIC_SOT, PUBLIC_SOT] as Set

    /**
     * Retrieves tokens for the studies to which the user has at least the required access level.
     * @return the set of study tokens.
     */
    static Set<String> getStudyTokensForUserWithMinimalPatientDataAccessLevel(User user, PatientDataAccessLevel requiredAccessLevel) {
        user.studyToPatientDataAccessLevel.entrySet().stream()
                .filter({ Map.Entry<String, PatientDataAccessLevel> entry ->
                    entry.value >= requiredAccessLevel
                })
                .map({ Map.Entry<String, PatientDataAccessLevel> entry -> entry.key })
                .collect(Collectors.toSet()) + PUBLIC_TOKENS
    }

    /**
     * Retrieves tokens for the studies to which the user has the patient data access level.
     * @return the set of study tokens.
     */
    static Set<String> getStudyTokensForUserWithPatientDataAccessLevel(User user, PatientDataAccessLevel patientDataAccessLevel) {
        user.studyToPatientDataAccessLevel.entrySet().stream()
                .filter({ Map.Entry<String, PatientDataAccessLevel> entry -> entry.value == patientDataAccessLevel })
                .map({ Map.Entry<String, PatientDataAccessLevel> entry -> entry.key })
                .collect(Collectors.toSet())
    }

    /**
     * Creates copy of the original user with changes permissions
     * @param user user to copy
     * @param fromAccLvl patient access level to replace
     * @param toAccLvl patient access level to replace with
     * @return a copy of the user with modified permissions
     */
    static User copyUserWithChangedPatientDataAccessLevel(User user, PatientDataAccessLevel fromAccLvl, PatientDataAccessLevel toAccLvl) {
        if (user.studyToPatientDataAccessLevel) {
            def newStudyToPatientDataAccessLevel = user.studyToPatientDataAccessLevel.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                    { Map.Entry<String, PatientDataAccessLevel> entry -> entry.key } as Function<Map.Entry<String, PatientDataAccessLevel>, String>,
                    { Map.Entry<String, PatientDataAccessLevel> entry -> entry.value == fromAccLvl ? toAccLvl : entry.value } as Function<Map.Entry<String, PatientDataAccessLevel>, PatientDataAccessLevel>,
            ))
            return new SimpleUser(
                    username: user.username,
                    realName: user.realName,
                    email: user.email,
                    admin: user.admin,
                    studyToPatientDataAccessLevel: newStudyToPatientDataAccessLevel
            )
        }
        return user
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
