package org.transmartproject.db.clinical

import org.transmartproject.core.multidimquery.query.*
import spock.lang.Specification

class ThresholdHelperSpec extends Specification {

    Constraint constraintMock = Mock(Constraint)

    def 'constraint limited to patients of studies'() {
        expect:
        ThresholdHelper.getConstraintLimitedToStudyPatients(constraintMock, ['study1', 'study2'] as Set) ==
                new AndConstraint([
                        constraintMock,
                        new SubSelectionConstraint(
                                'patient',
                                new OrConstraint([
                                        new StudyNameConstraint('study1'),
                                        new StudyNameConstraint('study2'),
                                ])
                        )]
                )
    }

}
