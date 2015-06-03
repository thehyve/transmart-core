package org.transmartproject.batch.i2b2.variable

import org.transmartproject.batch.support.ConfigurableLengthSemanticsTrait

import static org.transmartproject.batch.i2b2.variable.DimensionI2b2Variable.I2b2DimensionVariableType.*

/**
 * Columns of patient_dimension.
 */
enum PatientDimensionI2b2Variable implements DimensionI2b2Variable, ConfigurableLengthSemanticsTrait {

    VITAL_STATUS     ([:], ENUMERATION_LOOKUP, 'vital_status_cd'),
    BIRTH_DATE       ([:], DATE, 'birth_date'),
    DEATH_DATE       ([:], DATE, 'death_date'),
    SEX              ([:], ENUMERATION_LOOKUP, 'sex_cd'),
    AGE_IN_YEARS_NUM (     INTEGER, 'age_in_years_num', minValue: 0),
    LANGUAGE         ([:], ENUMERATION_LOOKUP, 'language_cd'),
    RACE             ([:], ENUMERATION_LOOKUP, 'race_cd'),
    MARITAL_STATUS   ([:], ENUMERATION_LOOKUP, 'marital_status_cd'),
    RELIGION         ([:], ENUMERATION_LOOKUP, 'religion_cd'),
    ZIP              (     STRING, 'zip_cd', maxSize: 50), // maybe ENUMERATION_LOOKUP but not in default data
    STATECITYZIP_PATH(     STRING, 'statecityzip_path', maxSize: 700),
    INCOME           (     STRING, 'income_cd', maxSize: 50), // maybe ENUMERATION_LOOKUP but not in default data
    BLOB             ([:], STRING, 'patient_blob')

    public final static String PATIENT_DIMENSION_KEY = 'PAT'

    PatientDimensionI2b2Variable(Map<String, Object> parameters,
                                 DimensionI2b2Variable.I2b2DimensionVariableType type,
                                 String column) {
        this.parameters      = parameters
        this.variableType    = type
        this.dimensionKey    = PATIENT_DIMENSION_KEY
        this.dimensionTable  = 'patient_dimension'
        this.dimensionColumn = column
    }
}
