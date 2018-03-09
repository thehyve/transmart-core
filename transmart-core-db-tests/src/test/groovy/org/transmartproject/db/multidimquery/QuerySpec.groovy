package org.transmartproject.db.multidimquery

import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import org.transmartproject.core.multidimquery.query.Field
import org.transmartproject.core.multidimquery.query.Operator
import org.transmartproject.core.multidimquery.query.Type
import org.transmartproject.core.multidimquery.query.FieldConstraint
import spock.lang.Specification

import static org.transmartproject.db.multidimquery.DimensionImpl.*

@TestMixin(ControllerUnitTestMixin)
class QuerySpec extends Specification {

    Field patientIdField

    void setupData() {
        patientIdField = new Field(dimension: PATIENT, fieldName: 'patientId', type: Type.NUMERIC)
    }

    void 'test observation query class'() {
        setupData()
        when:
        FieldConstraint constraint = mockCommandObject(FieldConstraint)
        constraint.field = patientIdField
        constraint.operator = Operator.EQUALS
        constraint.value = -101

        then:
        constraint.hasOperator()
        constraint.hasTypeThatMatchesOperator()
        constraint.hasValueOfRightType()
        constraint.hasNotListOperatorOrListValue()
    }

    void 'test invalid value constraint'() {
        setupData()
        when:
        FieldConstraint constraint = mockCommandObject(FieldConstraint)
        constraint.field = patientIdField
        constraint.operator = Operator.EQUALS
        constraint.value = "Invalid patient id"

        then:
        !constraint.hasValueOfRightType()
    }

    void 'test constraint equality'() {
        setupData()
        when:
        FieldConstraint constraint1 = mockCommandObject(FieldConstraint)
        constraint1.field = patientIdField
        constraint1.operator = Operator.EQUALS
        constraint1.value = -101

        FieldConstraint constraint2 = mockCommandObject(FieldConstraint)
        constraint2.field = patientIdField
        constraint2.operator = Operator.EQUALS
        constraint2.value = -101

        then:
        constraint1 == constraint2
    }

}
