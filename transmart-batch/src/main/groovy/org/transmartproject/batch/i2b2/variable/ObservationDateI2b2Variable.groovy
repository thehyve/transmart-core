package org.transmartproject.batch.i2b2.variable

/**
 * Represents the date columns in observation_fact.
 */
enum ObservationDateI2b2Variable implements I2b2Variable {
    START_DATE,
    END_DATE

    final boolean admittingFactValues = false
}
