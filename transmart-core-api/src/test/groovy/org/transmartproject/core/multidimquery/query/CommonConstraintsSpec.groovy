package org.transmartproject.core.multidimquery.query

import spock.lang.Specification

class CommonConstraintsSpec extends Specification {

    def 'constraint limited to patients of studies'() {
        Constraint constraint = new ValueConstraint(
                valueType: Type.STRING,
                operator: Operator.EQUALS,
                value: 'test')

        expect:
        CommonConstraints.getConstraintLimitedToStudyPatients(constraint, ['study1', 'study2'] as Set) ==
                new AndConstraint([
                        constraint,
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
