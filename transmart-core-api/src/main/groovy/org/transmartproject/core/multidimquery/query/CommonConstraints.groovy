package org.transmartproject.core.multidimquery.query

import java.util.stream.Collectors

/**
 * Contains methods to build common composite constraints
 */
class CommonConstraints {

    /**
     * Applies constraint to patients only observed under list of studies
     * @param constraint contraint to apply
     * @param studyNames limit to patients observed in following studies
     * @return composit constraint
     */
    static Constraint getConstraintLimitedToStudyPatients(Constraint constraint, Set<String> studyNames) {
        List<Constraint> cTSTudyNameConstraints = studyNames.stream()
                .map({ String studyName -> new StudyNameConstraint(studyName) }).collect(Collectors.toList())
        SubSelectionConstraint patientsFromSTudiesConstraint = new SubSelectionConstraint(
                dimension: 'patient',
                constraint: new OrConstraint(cTSTudyNameConstraints))
        return new AndConstraint([constraint, patientsFromSTudiesConstraint])
    }
}
