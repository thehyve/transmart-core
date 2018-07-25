package org.transmartproject.core.multidimquery.query

import com.fasterxml.jackson.databind.ObjectMapper
import org.transmartproject.core.binding.BindingException
import org.transmartproject.core.binding.BindingHelper
import spock.lang.Specification
import spock.lang.Unroll

class QuerySpec extends Specification {

    Field patientIdField = new Field(dimension: 'patient', fieldName: 'patientId', type: Type.NUMERIC)

    void 'test field constraint class'() {
        when:
        def constraint = new FieldConstraint()
        constraint.field = patientIdField
        constraint.operator = Operator.EQUALS
        constraint.value = -101

        then:
        constraint.hasOperator()
        constraint.hasTypeThatMatchesOperator()
        constraint.hasValueOfRightType()
        constraint.hasNotListOperatorOrListValue()
    }

    void 'test invalid value constraint in field constraints'() {
        when:
        FieldConstraint constraint = new FieldConstraint()
        constraint.field = patientIdField
        constraint.operator = Operator.EQUALS
        constraint.value = "Invalid patient id"

        then:
        !constraint.hasValueOfRightType()

        when: 'validating the constraint'
        BindingHelper.validate(constraint)

        then:
        BindingException e = thrown()
    }

    void 'test concept dimension is rejected in field constraints'() {
        when:
        def constraint = new FieldConstraint()
        constraint.field = new Field(dimension: 'concept', fieldName: 'conceptCode', type: Type.STRING)
        constraint.operator = Operator.EQUALS
        constraint.value = 'TEST'

        then:
        !constraint.hasNoConceptDimension()

        when: 'validating the constraint'
        BindingHelper.validate(constraint)

        then: 'An exception is thrown'
        BindingException e = thrown()
        e.message.contains('Concept dimension not allowed in field constraints.')
    }

    void 'test study dimension is rejected in field constraints'() {
        when:
        def constraint = new FieldConstraint()
        constraint.field = new Field(dimension: 'study', fieldName: 'studyId', type: Type.STRING)
        constraint.operator = Operator.EQUALS
        constraint.value = 'TEST'

        then:
        !constraint.hasNoStudyDimension()

        when: 'validating the constraint'
        BindingHelper.validate(constraint)

        then: 'An exception is thrown'
        BindingException e = thrown()
        e.message.contains('Study dimension not allowed in field constraints.')
    }

    void 'test study field is rejected in field constraints'() {
        when:
        def constraint = new FieldConstraint()
        constraint.field = new Field(dimension: 'trial visit', fieldName: 'study', type: Type.STRING)
        constraint.operator = Operator.EQUALS
        constraint.value = 'TEST'

        then:
        !constraint.hasNoTrialVisitStudyField()

        when: 'validating the constraint'
        BindingHelper.validate(constraint)

        then: 'An exception is thrown'
        BindingException e = thrown()
        e.message.contains('Field \'study\' of trial visit dimension not allowed in field constraints.')
    }

    void 'test field constraint equality'() {
        when:
        def constraint1 = new FieldConstraint()
        constraint1.field = patientIdField
        constraint1.operator = Operator.EQUALS
        constraint1.value = -101

        def constraint2 = new FieldConstraint()
        constraint2.field = patientIdField
        constraint2.operator = Operator.EQUALS
        constraint2.value = -101

        then:
        constraint1 == constraint2
    }

    @Unroll
    void 'test constraint serialisation and deserialisation with Jackson'(Constraint constraint) {
        expect:
        def mapper = new ObjectMapper()
        def json = mapper.writeValueAsString(constraint)
        // println "JSON: ${json}"
        def object = mapper.readValue(json, Constraint)
        assert constraint == object

        where:
        _| constraint
        _| new TrueConstraint()
        _| new ConceptConstraint('concept1')
        _| new StudyNameConstraint('study1')
        _| new ValueConstraint(Type.NUMERIC, Operator.LESS_THAN, 10.5)
        _| new ValueConstraint(Type.STRING, Operator.CONTAINS, 'x')
        _| new AndConstraint([new ConceptConstraint('concept1'), new ValueConstraint(Type.STRING, Operator.EQUALS, 'value x')])
        _| new RelationConstraint('PAR', null, null, null)
        _| new RelationConstraint('CHI', new TrueConstraint(), true, false)
    }

    @Unroll
    void 'test constraint serialisation and deserialisation with toJson'(Constraint constraint) {
        expect:
        def json = constraint.toJson()
        def mapper = new ObjectMapper()
        def object = mapper.readValue(json, Constraint)
        assert constraint == object

        where:
        _| constraint
        _| new TrueConstraint()
        _| new ConceptConstraint('concept1')
        _| new StudyNameConstraint('study1')
        _| new ValueConstraint(Type.NUMERIC, Operator.LESS_THAN, 10.5)
        _| new ValueConstraint(Type.STRING, Operator.CONTAINS, 'x')
        _| new AndConstraint([new ConceptConstraint('concept1'), new ValueConstraint(Type.STRING, Operator.EQUALS, 'value x')])
        _| new RelationConstraint('PAR', null, null, null)
        _| new RelationConstraint('CHI', new TrueConstraint(), true, false)
    }

}
