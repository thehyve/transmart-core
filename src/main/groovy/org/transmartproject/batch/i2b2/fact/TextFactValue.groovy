package org.transmartproject.batch.i2b2.fact

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Represents a fact of type {@link FactDataType#TEXT}.
 */
@ToString(includes = ['textValue', 'valueFlag'])
@EqualsAndHashCode(includes = ['textValue', 'valueFlag'])
@SuppressWarnings('DuplicateListLiteral')
class TextFactValue implements FactValue {

    /* settable */

    String textValue

    ValueFlag valueFlag

    /* end settable */

    final BigDecimal numberValue = null

    final FactDataType dataType = FactDataType.TEXT

    final String blob = null
}
