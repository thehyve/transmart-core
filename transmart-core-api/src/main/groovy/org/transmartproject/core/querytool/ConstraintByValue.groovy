package org.transmartproject.core.querytool

import groovy.transform.Immutable

@Immutable
class ConstraintByValue {

    /**
     * The operator that will be used to compare the value with the constraint.
     */
    Operator operator

    /**
     * The second operand in the constraint.
     */
    String constraint

    /**
     * The type of constraint. Indicates whether constraint is a number or a
     * flag like 'H' (high).
     */
    ValueType valueType


    enum Operator {

        LOWER_THAN          ('LT'),
        LOWER_OR_EQUAL_TO   ('LE'),
        EQUAL_TO            ('EQ'),
        BETWEEN             ('BETWEEN'),
        GREATER_THAN        ('GT'),
        GREATER_OR_EQUAL_TO ('GE');

        final String value

        protected Operator(String value) {
            this.value = value
        }

        static Operator forValue(String value) {
            values().find { value == it.value } ?:
                { throw new IllegalArgumentException("No operator for value $value") }()
        }
    }

    enum ValueType {
        NUMBER,
        FLAG
    }

}
