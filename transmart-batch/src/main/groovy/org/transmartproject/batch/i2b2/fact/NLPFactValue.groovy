package org.transmartproject.batch.i2b2.fact

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * A large structured text value.
 */
@ToString(includes = ['blob', 'valueFlag'])
@EqualsAndHashCode(includes = ['blob', 'valueFlag'])
@SuppressWarnings('DuplicateListLiteral')
class NLPFactValue implements FactValue {

    final String textValue = null

    final BigDecimal numberValue = null

    final FactDataType dataType = FactDataType.BLOB

    ValueFlag valueFlag

    String blob // should be XML
}
