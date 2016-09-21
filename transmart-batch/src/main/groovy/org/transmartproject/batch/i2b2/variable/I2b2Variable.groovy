package org.transmartproject.batch.i2b2.variable

import org.transmartproject.batch.i2b2.mapping.I2b2MappingEntry

/**
 * Represents a quantity in the i2b2 database that can be mapped with an
 * {@link I2b2MappingEntry}.
 */
interface I2b2Variable {
    boolean isAdmittingFactValues()
}
