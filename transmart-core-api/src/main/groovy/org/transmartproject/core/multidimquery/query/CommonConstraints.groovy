package org.transmartproject.core.multidimquery.query

import org.transmartproject.core.ontology.MDStudy

import java.util.stream.Collectors

/**
 * Contains methods to build common composite constraints
 */
class CommonConstraints {

    /**
     * Applies constraint to patients only observed under list of studies
     * @param constraint constraint to apply
     * @param studies limit to patients observed in following studies
     * @return composite constraint
     */
    static Constraint getConstraintLimitedToStudyPatients(Constraint constraint, Collection<MDStudy> studies) {
        List<Constraint> cTStudyNameConstraints = studies.stream()
                .map({ MDStudy study -> new StudyNameConstraint(study.name) }).collect(Collectors.toList())
        SubSelectionConstraint patientsFromStudiesConstraint = new SubSelectionConstraint(
                dimension: 'patient',
                constraint: new OrConstraint(cTStudyNameConstraints))
        return new AndConstraint([constraint, patientsFromStudiesConstraint])
    }

}
