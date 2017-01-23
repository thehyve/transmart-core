package org.transmartproject.batch.i2b2.variable

import static org.transmartproject.batch.i2b2.variable.DimensionI2b2Variable.I2b2DimensionVariableType.*

/**
 * Columns of the visit_dimension table.
 */
enum VisitDimensionI2b2Variable implements DimensionI2b2Variable {

    ACTIVE_STATUS(ENUMERATION, 'active_status_cd', values: [
            'F', // final
            'P', // preliminary
            'A', // active (no end date)
    ]),
    START_DATE    ([:], DATE,    'start_date'),
    END_DATE      ([:], DATE,    'end_date'),
    INOUT         (     STRING,  'inout_cd',      maxSize: 50), // maybe ENUMERATION_LOOKUP but not in default data
    LOCATION      (     STRING,  'location_cd',   maxSize: 50), // idem
    LOCATION_PATH (     STRING,  'location_path', maxSize: 900),
    LENGTH_OF_STAY([:], INTEGER, 'length_of_stay'),
    BLOB          ([:], STRING,  'visit_blob')

    public final static String VISITS_DIMENSION_KEY = 'VIS'

    VisitDimensionI2b2Variable(Map<String, Object> parameters,
                               DimensionI2b2Variable.I2b2DimensionVariableType type,
                               String column) {
        this.parameters      = parameters
        this.variableType    = type
        this.dimensionKey    = VISITS_DIMENSION_KEY
        this.dimensionTable  = 'visit_dimension'
        this.dimensionColumn = column
    }
}
