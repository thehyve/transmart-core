package org.transmartproject.batch.i2b2.variable

/**
 * An i2b2 variable that maps to a column in a dimension table.
 */
@SuppressWarnings('BracesForClassRule') // buggy with traits
trait DimensionI2b2Variable implements I2b2Variable {

    boolean isAdmittingFactValues() {
        false
    }

    String dimensionTable /* unqualified */

    String dimensionColumn

    I2b2DimensionVariableType variableType

    Map<String, Object> parameters

    // variable will be identified in mapping file with <tableKey>:<name>

    String dimensionKey

    abstract String name()

    String getKey() {
        "$dimensionKey:${name()}"
    }

    static enum I2b2DimensionVariableType {
        DATE,
        STRING,
        INTEGER,
        ENUMERATION,
        ENUMERATION_LOOKUP // look in i2b2demodata.code_lookup
    }
}
