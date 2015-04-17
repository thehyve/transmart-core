package org.transmartproject.batch.i2b2.mapping

import groovy.transform.ToString
import org.springframework.core.io.Resource
import org.transmartproject.batch.i2b2.fact.FactDataType
import org.transmartproject.batch.i2b2.variable.I2b2Variable

/**
 * Representation of a line in an i2b2 column mapping file.
 */
@ToString(includes = ['filename', 'columnNumber', 'variable', 'type', 'unit', 'serial'],
        includePackage = false)
class I2b2MappingEntry {

    /* Filled directly from text file */
    String filename

    Integer columnNumber

    String variable /* raw */

    String type /* raw */

    String unit

    boolean mandatory = true

    /* calculated */
    FactDataType getDataType() {
        FactDataType.fromInputFileRepresentation type // null if bad type
    }

    /* to be set later */
    Integer serial

    I2b2Variable i2b2Variable

    Resource fileResource
}
