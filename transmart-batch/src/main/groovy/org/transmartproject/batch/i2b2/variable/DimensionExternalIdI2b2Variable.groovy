package org.transmartproject.batch.i2b2.variable

import static org.transmartproject.batch.i2b2.variable.PatientDimensionI2b2Variable.PATIENT_DIMENSION_KEY
import static org.transmartproject.batch.i2b2.variable.ProviderDimensionI2b2Variable.PROVIDER_DIMENSION_KEY
import static org.transmartproject.batch.i2b2.variable.VisitDimensionI2b2Variable.VISITS_DIMENSION_KEY

/**
 * Represents a column that uniquely identifies a dimension row.
 */
enum DimensionExternalIdI2b2Variable implements I2b2Variable {

    PATIENT_EXTERNAL_ID (PATIENT_DIMENSION_KEY),     // put in patient mapping
    VISIT_EXTERNAL_ID   (VISITS_DIMENSION_KEY),      // put in visit mapping
    PROVIDER_EXTERNAL_ID(PROVIDER_DIMENSION_KEY, 50) // used directly as provider id

    final String dimensionKey

    final String key

    final int maxSize

    final boolean admittingFactValues = false

    DimensionExternalIdI2b2Variable(String dimensionKey, int maxSize = 200) {
        this.dimensionKey = dimensionKey
        this.key = "$dimensionKey:EID"
        this.maxSize = maxSize
    }
}
