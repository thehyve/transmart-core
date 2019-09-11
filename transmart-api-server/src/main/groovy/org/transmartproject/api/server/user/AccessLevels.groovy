package org.transmartproject.api.server.user

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.transmartproject.core.users.PatientDataAccessLevel

import java.text.ParseException

@Slf4j
@CompileStatic
class AccessLevels {

    public static final String ROLE_ADMIN = 'ROLE_ADMIN'
    public static final String ROLE_PUBLIC = 'ROLE_PUBLIC'

    private static Tuple2<String, PatientDataAccessLevel> parseStudyTokenToAccessLevel(String studyTokenToAccLvl) {
        String[] studyTokenToAccLvlSplit = studyTokenToAccLvl.split('\\|')
        if (studyTokenToAccLvlSplit.length != 2) {
            throw new ParseException("Can't parse permission '${studyTokenToAccLvl}'.", 0)
        }

        String studyToken = studyTokenToAccLvlSplit[0]
        if (!studyToken) {
            throw new IllegalArgumentException("Empty study: '${studyTokenToAccLvl}'.")
        }
        String accessLevel = studyTokenToAccLvlSplit[1]
        new Tuple2(studyToken, PatientDataAccessLevel.valueOf(accessLevel))
    }

    /**
     * Converts a list of Spring authorities, represented as String, in the form
     * `<STUDY_ID>|<ACCESS_LEVEL>` to a map from studies to access levels.
     * The study ID is a string, the access level needs to be one of the access
     * levels defined in {@link PatientDataAccessLevel}.
     * Example: `STUDY_A|MEASUREMENTS` means full access to the study with id STUDY_A.
     *
     * If multiple access levels are specified per study, the most permissive level is selected.
     * Invalid entries are ignored.
     *
     * @param roles the list of Spring authorities as string.
     * @return a map from study id to the highest access level granted for that study.
     */
    static Map<String, PatientDataAccessLevel> buildStudyToPatientDataAccessLevel(
            final Collection<String> roles) {
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

    /**
     * Checks if the list of authorities contains any value that represents
     * a valid access level in TranSMART, i.e., either {@link #ROLE_ADMIN},
     * {@link #ROLE_PUBLIC} or the combination of study id and {@link PatientDataAccessLevel}.
     *
     * @param authorities the list of Spring authorities.
     * @return true iff the list contains any of {@link #ROLE_ADMIN}, {@link #ROLE_PUBLIC}
     * or a valid combination of study id and {@link PatientDataAccessLevel}.
     */
    static boolean hasAuthorities(List<String> authorities) {
        if (authorities.empty) {
            return false
        }
        if (authorities.remove(ROLE_PUBLIC) || authorities.remove(ROLE_ADMIN)) {
            return true
        }
        Map<String, PatientDataAccessLevel> studyToPatientDataAccessLevel = buildStudyToPatientDataAccessLevel(authorities)
        return !studyToPatientDataAccessLevel.empty
    }

}
