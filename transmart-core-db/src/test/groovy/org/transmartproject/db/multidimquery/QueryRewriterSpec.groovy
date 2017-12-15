package org.transmartproject.db.multidimquery

import org.transmartproject.db.multidimquery.query.AndConstraint
import org.transmartproject.db.multidimquery.query.CombinationConstraintRewriter
import org.transmartproject.db.multidimquery.query.ConceptConstraint
import org.transmartproject.db.multidimquery.query.Constraint
import org.transmartproject.db.multidimquery.query.Negation
import org.transmartproject.db.multidimquery.query.Operator
import org.transmartproject.db.multidimquery.query.OrConstraint
import org.transmartproject.db.multidimquery.query.PatientSetConstraint
import org.transmartproject.db.multidimquery.query.StudyNameConstraint
import org.transmartproject.db.multidimquery.query.SubSelectionConstraint
import org.transmartproject.db.multidimquery.query.TemporalConstraint
import org.transmartproject.db.multidimquery.query.TrueConstraint
import org.transmartproject.db.multidimquery.query.Type
import org.transmartproject.db.multidimquery.query.ValueConstraint
import spock.lang.Specification

class QueryRewriterSpec extends Specification {

    void 'test constraint rewriting of multiple concept constraints'() {
        given: 'two logically equivalent constraints, the second form preferred'
        Constraint constraint = new OrConstraint([
                new ConceptConstraint('height'),
                new ConceptConstraint('birthdate')
        ])

        Constraint expected = new ConceptConstraint(conceptCodes: ['height', 'birthdate'])

        when: 'rewriting the first constraint'
        def result = new CombinationConstraintRewriter().build(constraint)

        then: 'the rewrite result is equal to the preferred form'
        result.toJson() == expected.toJson()
    }

    void 'test constraint rewriting of study specific constraints'() {
        given: 'two logically equivalent constraints, the second form preferred'
        Constraint constraint = new OrConstraint([
                new AndConstraint([
                        new StudyNameConstraint('SURVEY0'),
                        new ConceptConstraint('height')
                ]),
                new AndConstraint([
                        new StudyNameConstraint('SURVEY0'),
                        new ConceptConstraint('birthdate')
                ])
        ])
        Constraint expected = new AndConstraint([
                new StudyNameConstraint('SURVEY0'),
                new ConceptConstraint(conceptCodes:['height', 'birthdate'])
        ])

        when: 'rewriting the first constraint'
        def result = new CombinationConstraintRewriter().build(constraint)

        then: 'the rewrite result is equal to the preferred form'
        result.toJson() == expected.toJson()
    }

    void 'test eliminate double negation'() {
        given: 'a constraint with double negation and its simplified form'
        Constraint constraint = new AndConstraint([
                new StudyNameConstraint('SURVEY0'),
                new Negation(new Negation(new AndConstraint([
                        new ConceptConstraint('height'),
                        new StudyNameConstraint('SURVEY0')
                ])))
        ])

        Constraint expected = new AndConstraint([
                new StudyNameConstraint('SURVEY0'),
                new ConceptConstraint('height'),
                new StudyNameConstraint('SURVEY0'),
        ])

        when: 'rewriting the first constraint'
        def result = new CombinationConstraintRewriter().build(constraint)

        then: 'the rewrite result is equal to the preferred form'
        result.toJson() == expected.toJson()
    }

    void 'test simplification of singleton combinations'() {
        given: 'a two level singleton combination with a concept constraint and its simplified form'
        Constraint constraint = new AndConstraint([
                new OrConstraint([
                        new ConceptConstraint('height')
                ])
        ])

        Constraint expected = new ConceptConstraint('height')

        when: 'rewriting the first constraint'
        def result = new CombinationConstraintRewriter().build(constraint)

        then: 'the rewrite result is equal to the preferred form'
        result.toJson() == expected.toJson()
    }

    void 'test simplification of nested combinations'() {
        given: 'a constraint with double negation'
        Constraint constraint = new AndConstraint([
                new AndConstraint([
                        new StudyNameConstraint('SURVEY1'),
                        new ConceptConstraint('height')
                ]),
                new AndConstraint([
                        new PatientSetConstraint(patientSetId: -1),
                        new TrueConstraint()
                ])
        ])

        Constraint expected = new AndConstraint([
                new StudyNameConstraint('SURVEY1'),
                new ConceptConstraint('height'),
                new PatientSetConstraint(patientSetId: -1)
        ])

        when: 'rewriting the first constraint'
        def result = new CombinationConstraintRewriter().build(constraint)

        then: 'the rewrite result is equal to the preferred form'
        result.toJson() == expected.toJson()
    }

    void 'test rewriting of nested constraints'() {
        given: 'a nested constraint and its simplified form'
        Constraint constraint = new TemporalConstraint(
                Operator.EXISTS,
                new OrConstraint([
                        new AndConstraint([
                                new StudyNameConstraint('SURVEY0'),
                                new ConceptConstraint('height')
                        ]),
                        new AndConstraint([
                                new StudyNameConstraint('SURVEY0'),
                                new ConceptConstraint('birthdate')
                        ])
                ]))
        Constraint expected = new TemporalConstraint(
                Operator.EXISTS,
                new AndConstraint([
                        new StudyNameConstraint('SURVEY0'),
                        new ConceptConstraint(conceptCodes:['height', 'birthdate'])
                ]))

        when: 'rewriting the first constraint'
        def result = new CombinationConstraintRewriter().build(constraint)

        then: 'the rewrite result is equal to the preferred form'
        result.toJson() == expected.toJson()
    }

    void 'test patient and variable selection constraint'() {
        given: 'a patient and variable selection constraint and its simplified form'
        Constraint constraint = new AndConstraint([
                    new AndConstraint([
                            new SubSelectionConstraint("patient",
                                new AndConstraint([
                                        new ConceptConstraint("PRECOMP:IS_TWIN"),
                                        new StudyNameConstraint("PEDIGREE_VARS")
                                ])
                            ),
                            new SubSelectionConstraint("patient",
                                    new AndConstraint([
                                            new AndConstraint([
                                                new ConceptConstraint("height"),
                                                new ValueConstraint(Type.NUMERIC, Operator.EQUALS, 170)
                                            ]),
                                            new StudyNameConstraint("SURVEY1"),
                                    ])
                            )
                    ]),
                    new OrConstraint([
                            new AndConstraint([
                                    new ConceptConstraint("PRECOMP:IS_TWIN"),
                                    new StudyNameConstraint("PEDIGREE_VARS")
                            ]),
                            new AndConstraint([
                                    new ConceptConstraint("age"),
                                    new StudyNameConstraint("SURVEY2")
                            ]),
                            new AndConstraint([
                                    new ConceptConstraint("favouritebook"),
                                    new StudyNameConstraint("SURVEY2")
                            ])
                    ])
            ])

        Constraint expected = new AndConstraint([
               new SubSelectionConstraint("patient",
                        new AndConstraint([
                                new ConceptConstraint("PRECOMP:IS_TWIN"),
                                new StudyNameConstraint("PEDIGREE_VARS")
                        ])
                ),
                new SubSelectionConstraint("patient",
                        new AndConstraint([
                                new ConceptConstraint("height"),
                                new ValueConstraint(Type.NUMERIC, Operator.EQUALS, 170),
                                new StudyNameConstraint("SURVEY1"),
                        ])
                ),
                new OrConstraint([
                        new AndConstraint([
                                new StudyNameConstraint("PEDIGREE_VARS"),
                                new ConceptConstraint("PRECOMP:IS_TWIN")
                        ]),
                        new AndConstraint([
                                new StudyNameConstraint("SURVEY2"),
                                new ConceptConstraint(conceptCodes: [ "age", "favouritebook"])
                        ])
                ])
        ])

        when: 'rewriting the first constraint'
        def result = new CombinationConstraintRewriter().build(constraint)

        then: 'the rewrite result is equal to the preferred form'
        result.toJson() == expected.toJson()
    }

    void 'test rewriting of subselect with true subconstraint'() {
        given: 'two logically equivalent constraints, the second form preferred'
        Constraint constraint = new SubSelectionConstraint('patient', new TrueConstraint())

        Constraint expected = new TrueConstraint()

        when: 'rewriting the first constraint'
        def result = new CombinationConstraintRewriter().build(constraint)

        then: 'the rewrite result is equal to the preferred form'
        result.toJson() == expected.toJson()
    }

    void 'test rewriting of conjunction with subselect with true subconstraint'() {
        given: 'two logically equivalent constraints, the second form preferred'
        Constraint constraint = new AndConstraint([
                new SubSelectionConstraint('patient', new TrueConstraint()),
                new ConceptConstraint('favouritebook')
        ])

        Constraint expected = new ConceptConstraint('favouritebook')

        when: 'rewriting the first constraint'
        def result = new CombinationConstraintRewriter().build(constraint)

        then: 'the rewrite result is equal to the preferred form'
        result.toJson() == expected.toJson()
    }

    void 'test rewriting of nested subselect with true subconstraint'() {
        given: 'two logically equivalent constraints, the second form preferred'
        Constraint constraint = new SubSelectionConstraint('patient',
                new SubSelectionConstraint('patient', new TrueConstraint()))

        Constraint expected = new TrueConstraint()

        when: 'rewriting the first constraint'
        def result = new CombinationConstraintRewriter().build(constraint)

        then: 'the rewrite result is equal to the preferred form'
        result.toJson() == expected.toJson()
    }

}
