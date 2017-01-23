package org.transmartproject.batch.i2b2.fact

import com.google.common.collect.ImmutableMap
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * A numeric value.
 */
@ToString(includes = ['operator', 'numberValue', 'valueFlag'])
@EqualsAndHashCode(includes = ['operator', 'numberValue', 'valueFlag'])
@SuppressWarnings('DuplicateListLiteral')
class NumberFactValue implements FactValue {

    enum NumberFactOperator {
        LOWER_OR_EQUAL('<=', 'LE'),
        LOWER('<', 'L'),
        EQUAL('=', 'E'),
        NOT_EQUAL('!=', 'NE'),
        GREATER('>', 'G'),
        GREATER_OR_EQUAL('>=', 'GE'),

        final String dataFileRepresentation

        final String databaseValue

        NumberFactOperator(String dataFileRepresentation,
                           String databaseValue) {
            this.dataFileRepresentation = dataFileRepresentation
            this.databaseValue = databaseValue
        }

        private static final Map<String, NumberFactOperator> INDEX = {
            def builder = ImmutableMap.builder()
            values().each {
                builder.put(it.dataFileRepresentation, it)
            }
            builder.build()
        }()

        static NumberFactOperator forString(String s) {
            INDEX.get(s)
        }
    }

    /* settable */

    NumberFactOperator operator

    ValueFlag valueFlag

    BigDecimal numberValue

    /* end settable */

    @Override
    String getTextValue() {
        operator?.databaseValue ?: NumberFactOperator.EQUAL.databaseValue
    }

    final String blob = null

    final FactDataType dataType = FactDataType.NUMBER
}
