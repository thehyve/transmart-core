package org.transmartproject.core.multidimquery.query

import org.transmartproject.core.ontology.MDStudy
import spock.lang.Specification

class CommonConstraintsSpec extends Specification {

    def 'constraint limited to patients of studies'() {
        Constraint constraint = new ValueConstraint(
                valueType: Type.STRING,
                operator: Operator.EQUALS,
                value: 'test')
        def study1 = Mock(MDStudy)
        study1.name >> 'study1'
        def study2 = Mock(MDStudy)
        study2.name >> 'study2'

        expect:
        CommonConstraints.getConstraintLimitedToStudyPatients(constraint, [study1, study2] as Set) ==
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
