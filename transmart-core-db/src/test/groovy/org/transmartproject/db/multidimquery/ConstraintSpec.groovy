package org.transmartproject.db.multidimquery

import org.transmartproject.db.multidimquery.query.*
import spock.lang.Specification

import java.text.SimpleDateFormat

class ConstraintSpec extends Specification {

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
