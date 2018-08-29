package org.transmartproject.db.clinical

import groovy.transform.CompileStatic
import org.transmartproject.core.multidimquery.query.*
import org.transmartproject.core.users.PatientDataAccessLevel
import org.transmartproject.core.users.SimpleUser
import org.transmartproject.core.users.User

import java.util.function.Function
import java.util.stream.Collectors

import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS
import static org.transmartproject.core.users.PatientDataAccessLevel.COUNTS_WITH_THRESHOLD

@CompileStatic
class ThresholdHelper {

    static User copyUserWithAccessToExactCounts(User user) {
        if (user.studyToPatientDataAccessLevel) {
            def newStudyToPatientDataAccessLevel = user.studyToPatientDataAccessLevel.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                    { Map.Entry<String, PatientDataAccessLevel> entry -> entry.key } as Function<Map.Entry<String, PatientDataAccessLevel>, String>,
                    { Map.Entry<String, PatientDataAccessLevel> entry -> entry.value == COUNTS_WITH_THRESHOLD ? COUNTS : entry.value } as Function<Map.Entry<String, PatientDataAccessLevel>, PatientDataAccessLevel>,
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

    static boolean needsCountsWithThresholdCheck(User user) {
        (!user.admin
                && user.studyToPatientDataAccessLevel
                && user.studyToPatientDataAccessLevel.containsValue(COUNTS_WITH_THRESHOLD))
    }

    static Set getCountsWithThresholdStudyNames(User user) {
        return user.studyToPatientDataAccessLevel.entrySet().stream()
                .filter({ Map.Entry<String, PatientDataAccessLevel> entry -> entry.value == COUNTS_WITH_THRESHOLD })
                .map({ Map.Entry<String, PatientDataAccessLevel> entry -> entry.key })
                .collect(Collectors.toSet())
    }

    static Constraint getConstraintLimitedToStudyPatients(Constraint constraint, Set<String> studyNames) {
        List<Constraint> cTSTudyNameConstraints = studyNames.stream()
                .map({ String studyName -> new StudyNameConstraint(studyName) }).collect(Collectors.toList())
        SubSelectionConstraint patientsFromSTudiesConstraint = new SubSelectionConstraint(
                dimension: 'patient',
                constraint: new OrConstraint(cTSTudyNameConstraints))
        return new AndConstraint([constraint, patientsFromSTudiesConstraint])
    }

}
