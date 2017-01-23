package org.transmartproject.batch.i2b2.firstpass

import groovy.transform.ToString
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.core.io.Resource
import org.transmartproject.batch.i2b2.mapping.I2b2MappingEntry

/**
 * Pair of {@link I2b2MappingEntry}, and string data read
 * from the {@link FieldSet} with {@link FieldSet#readString(int)}
 */
@ToString(includeNames = true)
class I2b2FirstPassDataPoint {
    I2b2MappingEntry entry

    Resource resource
    int line

    String data             // trimmed and nulled if empty
}
