package org.transmartproject.db.multidimquery

import org.transmartproject.core.multidimquery.query.AndConstraint
import org.transmartproject.core.multidimquery.query.CombinationConstraintRewriter
import org.transmartproject.core.multidimquery.query.ConceptConstraint
import org.transmartproject.core.multidimquery.query.Constraint
import org.transmartproject.core.multidimquery.query.Field
import org.transmartproject.core.multidimquery.query.Negation
import org.transmartproject.core.multidimquery.query.Operator
import org.transmartproject.core.multidimquery.query.OrConstraint
import org.transmartproject.core.multidimquery.query.PatientSetConstraint
import org.transmartproject.core.multidimquery.query.StudyNameConstraint
import org.transmartproject.core.multidimquery.query.SubSelectionConstraint
import org.transmartproject.core.multidimquery.query.TemporalConstraint
import org.transmartproject.core.multidimquery.query.TimeConstraint
import org.transmartproject.core.multidimquery.query.TrueConstraint
import org.transmartproject.core.multidimquery.query.Type
import org.transmartproject.core.multidimquery.query.ValueConstraint
import spock.lang.Specification

import java.time.Instant

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
                new ConceptConstraint(conceptCodes: ['birthdate', 'height'])
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
                        new ConceptConstraint(conceptCodes: ['birthdate', 'height'])
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

    void 'test canonising constraints'() {
        given: 'two logically equivalent constraints'
        Constraint constraint1 = new AndConstraint([
                new SubSelectionConstraint('patient', new TrueConstraint()),
                new ConceptConstraint('favouritebook')
        ])
        Constraint constraint2 = new AndConstraint([
                new ConceptConstraint('favouritebook'),
                new SubSelectionConstraint('patient', new TrueConstraint())
        ])

        when: 'canonising the constraings'
        def canonical1 = constraint1.canonise()
        def canonical2 = constraint2.canonise()

        then: 'they are the same'
        canonical1.toJson() == canonical2.toJson()
    }

    void 'test canonising nested constraints'() {
        given: 'two logically equivalent constraints'
        def t1 = Instant.parse('2000-01-01T13:37:01Z')
        def t2 = Instant.parse('2017-12-01T13:37:05Z')
        Constraint constraint1 = new AndConstraint([
                new OrConstraint([
                        new SubSelectionConstraint('patient', new AndConstraint([
                                new PatientSetConstraint(patientIds: [8, 5, 3, 1]),
                                new TimeConstraint(new Field('start time', Type.DATE, 'startDate'),
                                        Operator.BETWEEN, [Date.from(t1), Date.from(t2)]),
                        ])),
                        new ConceptConstraint('favouritebook')
                ]),
                new OrConstraint([
                        new StudyNameConstraint('SURVEY1'),
                        new StudyNameConstraint('SURVEY2')
                ])
        ])
        Constraint constraint2 = new AndConstraint([
                new OrConstraint([
                        new StudyNameConstraint('SURVEY2'),
                        new StudyNameConstraint('SURVEY1')
                ]),
                new OrConstraint([
                        new ConceptConstraint('favouritebook'),
                        new SubSelectionConstraint('patient', new AndConstraint([
                                new TimeConstraint(new Field('start time', Type.DATE, 'startDate'),
                                Operator.BETWEEN, [Date.from(t1), Date.from(t2)]),
                                new PatientSetConstraint(patientIds: [1, 3, 8, 5])
                        ]))
                ])
        ])

        when: 'canonising the constraings'
        def canonical1 = constraint1.canonise()
        def canonical2 = constraint2.canonise()

        then: 'they are the same'
        canonical1.toJson() == canonical2.toJson()
    }

    void 'test rewriting does not mutate constraints'() {
        given: 'a disjunction of conjunctions in the wrong order'
        Constraint constraint = new OrConstraint([
                new AndConstraint([
                        new StudyNameConstraint('SURVEY1'),
                        new ConceptConstraint('weight')
                ]),
                new AndConstraint([
                        new StudyNameConstraint('SURVEY1'),
                        new ConceptConstraint('height')
                ])
        ])

        when: 'rewriting the constraint'
        def normalisedConstraint = constraint.normalise()

        then: 'the rewrite result is different from the original'
        constraint.toJson() != normalisedConstraint.toJson()
    }

}
