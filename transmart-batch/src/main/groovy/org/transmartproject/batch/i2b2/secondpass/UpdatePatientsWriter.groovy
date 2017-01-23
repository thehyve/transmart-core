package org.transmartproject.batch.i2b2.secondpass

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.transmartproject.batch.i2b2.variable.DimensionI2b2Variable
import org.transmartproject.batch.i2b2.variable.PatientDimensionI2b2Variable

/**
 * Update columns in patietn_dimension.
 */
@Component
@JobScope
class UpdatePatientsWriter extends AbstractDimensionUpdateWriter {

    private static final String PATIENT_NUM = 'patient_num'

    private final Class<? extends DimensionI2b2Variable> enumClass =
            PatientDimensionI2b2Variable

    private final List<String> keys = [PATIENT_NUM]

    @Override
    protected Class<? extends DimensionI2b2Variable> getEnumClass() {
        enumClass
    }

    @Override
    protected List<String> getKeys() {
        keys
    }

    @Override
    protected Map<String, ?> keyValuesFromRow(I2b2SecondPassRow row) {
        [(PATIENT_NUM): row.patientNum]
    }

    @Override
    protected Map<PatientDimensionI2b2Variable, Object> dimensionValuesFromRow(
            I2b2SecondPassRow row) {
        row.patientDimensionValues
    }
}
