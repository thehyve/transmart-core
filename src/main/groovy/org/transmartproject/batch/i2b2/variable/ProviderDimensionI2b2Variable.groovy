package org.transmartproject.batch.i2b2.variable

import org.transmartproject.batch.clinical.db.objects.Tables

import static org.transmartproject.batch.i2b2.variable.DimensionI2b2Variable.I2b2DimensionVariableType.*

/**
 * Columns of the provider dimension table.
 */
enum ProviderDimensionI2b2Variable implements DimensionI2b2Variable {

    NAME_CHAR    (     STRING, 'name_char', maxSize: 850),
    BLOB         ([:], STRING, 'provider_blob')

    public final static String PROVIDER_DIMENSION_KEY = 'PRO'

    ProviderDimensionI2b2Variable(Map<String, Object> parameters,
                                  DimensionI2b2Variable.I2b2DimensionVariableType type,
                                  String column) {
        this.parameters      = parameters
        this.variableType    = type
        this.dimensionKey    = PROVIDER_DIMENSION_KEY
        this.dimensionTable  = Tables.PROV_DIMENSION
        this.dimensionColumn = column
    }

}
