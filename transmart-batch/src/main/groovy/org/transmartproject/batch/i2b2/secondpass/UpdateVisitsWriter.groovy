package org.transmartproject.batch.i2b2.secondpass

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.stereotype.Component
import org.transmartproject.batch.i2b2.variable.DimensionI2b2Variable
import org.transmartproject.batch.i2b2.variable.VisitDimensionI2b2Variable

/**
 * Update columns in visit_dimension.
 */
@Component
@JobScope
class UpdateVisitsWriter extends AbstractDimensionUpdateWriter {

    private static final String ENCOUNTER_NUM = 'encounter_num'
    private static final String PATIENT_NUM = 'patient_num'

    private final List<String> keys = [ENCOUNTER_NUM, PATIENT_NUM]

    @Override
    protected Class<? extends DimensionI2b2Variable> getEnumClass() {
        VisitDimensionI2b2Variable
    }

    @Override
    protected List<String> getKeys() {
        keys
    }

    @Override
    protected Map<String, ?> keyValuesFromRow(I2b2SecondPassRow row) {
        [
                (ENCOUNTER_NUM): row.encounterNum,
                (PATIENT_NUM)  : row.patientNum
        ]
    }

    @Override
    protected Map<VisitDimensionI2b2Variable, Object> dimensionValuesFromRow(
            I2b2SecondPassRow row) {
        row.visitDimensionValues
    }
}
