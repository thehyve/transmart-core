package org.transmartproject.db.dataquery2

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.Integration
import grails.test.mixin.web.ControllerUnitTestMixin
import org.transmartproject.db.dataquery2.query.Field
import org.transmartproject.db.dataquery2.query.ObservationQuery
import org.transmartproject.db.dataquery2.query.Operator
import org.transmartproject.db.dataquery2.query.QueryType
import org.transmartproject.db.dataquery2.query.Type
import org.transmartproject.db.dataquery2.query.FieldConstraint
import spock.lang.Specification

@Integration
@TestMixin(ControllerUnitTestMixin)
class QuerySpec extends Specification {

    Field patientIdField

    void setupData() {
        patientIdField = new Field(dimension: PatientDimension, fieldName: 'patientId', type: Type.NUMERIC)
    }

    void 'test observation query class'() {
        setupData()
        when:
        FieldConstraint constraint = mockCommandObject(FieldConstraint)
        constraint.field = patientIdField
        constraint.operator = Operator.EQUALS
        constraint.value = -101
        ObservationQuery query = mockCommandObject(ObservationQuery)
        query.queryType = QueryType.VALUES
        query.constraint = constraint

        then:
        query.validate()
    }

    void 'test invalid value constraint'() {
        setupData()
        when:
        FieldConstraint constraint = mockCommandObject(FieldConstraint)
        constraint.field = patientIdField
        constraint.operator = Operator.EQUALS
        constraint.value = "Invalid patient id"
        ObservationQuery query = mockCommandObject(ObservationQuery)
        query.queryType = QueryType.VALUES
        query.constraint = constraint

        then:
        !constraint.validate()
        constraint.errors.fieldErrorCount == 1
        constraint.errors.fieldErrors[0].field == 'value'
        constraint.errors.fieldErrors[0].code == 'org.transmartproject.query.invalid.value.message'

        !query.validate()
        query.errors.fieldErrorCount == 1
        query.errors.fieldErrors[0].field == 'constraint'
        query.errors.fieldErrors[0].code == 'org.transmartproject.query.invalid.constraint.message'
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
