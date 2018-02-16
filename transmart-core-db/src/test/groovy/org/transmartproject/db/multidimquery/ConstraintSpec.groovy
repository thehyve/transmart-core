package org.transmartproject.db.multidimquery

import org.transmartproject.db.multidimquery.query.*
import spock.lang.Specification

import java.text.SimpleDateFormat

class ConstraintSpec extends Specification {

    void 'test constraint factory casting objects to properties type'() {
        when: 'value is null'
        def null1 = ConstraintFactory.convertValueToPropertyType(String, null)
        then: 'null is returned'
        null1 == null

        when: 'property type and value compatible'
        def str1 = ConstraintFactory.convertValueToPropertyType(String, "Hello")
        then: 'the value just returns'
        str1 instanceof String

        when: 'property type is enum and string value supplied'
        def enum1 = ConstraintFactory.convertValueToPropertyType(Type, "event")
        then: 'the string is parsed correctly'
        enum1 == Type.EVENT

        when: 'property is operator and operator symbol supplied'
        def enum2 = ConstraintFactory.convertValueToPropertyType(Operator, ">")
        then: 'the operator is parsed correctly'
        enum2 == Operator.GREATER_THAN

        when: 'property is operator and operator name supplied'
        def enum3 = ConstraintFactory.convertValueToPropertyType(Operator, "before")
        then: 'the operator is parsed correctly'
        enum3 == Operator.BEFORE

        when: 'property type and value match'
        def long1 = ConstraintFactory.convertValueToPropertyType(Long, new Long(1))
        then: 'the value just returns'
        long1 instanceof Long

        when: 'property type of broader numerical (long) type'
        def long2 = ConstraintFactory.convertValueToPropertyType(Long, new Integer(1))
        then: 'the int value converted up'
        long2 instanceof Long

        when: 'property type of narrower numerical (int) type'
        def int1 = ConstraintFactory.convertValueToPropertyType(Integer, new Long(1))
        then: 'the long value converted down'
        int1 instanceof Integer

        when: 'date property type and string is supplied'
        def date1 = ConstraintFactory.convertValueToPropertyType(Date, toCompleteDateString('28-09-2010'))
        then: 'the date gets parsed'
        date1 instanceof Date
        date1 == toDate('28-09-2010')

        when: 'property type is set and list value supplied'
        def strSet1 = ConstraintFactory.convertValueToPropertyType(Set, ['a', 'b', 'c'])
        then: 'the value gets converted to list'
        strSet1 instanceof Set
        strSet1 == ['a', 'b', 'c'] as Set

        when: 'property type is field and map value supplied'
        def field1 = ConstraintFactory.convertValueToPropertyType(Field, [dimension: 'concept', type: 'string', fieldName: 'conceptPath'])
        then: 'the map is parsed correctly'
        field1 instanceof Field
        field1.dimension == 'concept'
        field1.type == Type.STRING
        field1.fieldName == 'conceptPath'

        when: 'property type is constraint and map value supplied'
        def constraint1 = ConstraintFactory.convertValueToPropertyType(ConceptConstraint, [type: 'concept', conceptCode: 'conceptCd'])
        then: 'the map is parsed correctly'
        constraint1 instanceof ConceptConstraint
        constraint1.conceptCode == 'conceptCd'


        when: 'property type is list and list with constraints supplied'
        def constraintList1 = ConstraintFactory.convertValueToPropertyType(List,
                [
                        [ type: 'concept', conceptCode: 'conceptCd1'],
                        [ type: 'concept', conceptCode: 'conceptCd2'],
                ])
        then: 'the constraints is parsed correctly'
        constraintList1 instanceof List
        constraintList1.size() == 2
        constraintList1[0] instanceof ConceptConstraint
        constraintList1[0].conceptCode == 'conceptCd1'
        constraintList1[1] instanceof ConceptConstraint
        constraintList1[1].conceptCode == 'conceptCd2'

        when: 'property is dates list and list of strings supplied'
        def datesList1 = ConstraintFactory.convertValueToPropertyType(TimeConstraint.class.getDeclaredField('values'),
                [toCompleteDateString('21-10-2017'), toCompleteDateString('13-12-2018')])
        then: 'the list is parsed correctly'
        datesList1 instanceof List
        datesList1 == [toDate('21-10-2017'), toDate('13-12-2018')]

        when: 'property is longs set and list of integer supplied'
        def longSet1 = ConstraintFactory.convertValueToPropertyType(PatientSetConstraint.class.getDeclaredField('patientIds'),
                [1, 2])
        then: 'the set is parsed correctly'
        longSet1 instanceof Set
        longSet1 == [1L, 2L] as Set
    }

    void 'test constraint type validation'() {
        when: 'valid constraint supplied'
        def validConstraint1 = ConstraintFactory.create([type: 'concept', conceptCode: 'conceptCd'])
        then: 'constraint object is created out of it'
        validConstraint1 instanceof ConceptConstraint
        validConstraint1.conceptCode == 'conceptCd'

        when: 'null constraint without field'
        ConstraintFactory.create([type: 'null'])
        then: 'constraint binding exception is thrown'
        def e1 = thrown(ConstraintBindingException)
        e1.message.contains('null')
        e1.message.contains('field')
        e1.errors
        e1.errors.size() == 1

        when: 'value constraint with 3 not compatible fileds supplied'
        ConstraintFactory.create([type: 'value', valueType: 'date', operator: 'or', value: 'text'])
        then: 'constraint binding exception is thrown with 3 violations'
        def e2 = thrown(ConstraintBindingException)
        e2.message.contains('Only string or numerical value type is allowed')
        e2.message.contains('The type does not support the value')
        e2.message.contains('The value type is not compatible with the operator')
        e2.errors
        e2.errors.size() == 3

        when: 'nested constraints supplied'
        def fieldConstraint1 = ConstraintFactory.create([
                type    : 'field',
                field   : [dimension: 'patient', type: 'string', fieldName: 'sourcesystemCd'],
                operator: 'contains',
                value   : 'SUBJ_ID_2'
        ])
        then: 'they are converted to nested constraint objects accordingly'
        fieldConstraint1 instanceof FieldConstraint
        fieldConstraint1.operator == Operator.CONTAINS
        fieldConstraint1.value == 'SUBJ_ID_2'
        fieldConstraint1.field instanceof Field
        fieldConstraint1.field.dimension == 'patient'
        fieldConstraint1.field.type == Type.STRING
        fieldConstraint1.field.fieldName == 'sourcesystemCd'
    }

    def toCompleteDateString(String dateString, String inputFormat = "dd-MM-yyyy") {
        toDate(dateString, inputFormat).format("yyyy-MM-dd'T'HH:mm:ss")
    }

    def toDate(String dateString, String inputFormat = "dd-MM-yyyy") {
        new SimpleDateFormat(inputFormat).parse(dateString)
    }

}
