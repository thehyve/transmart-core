package org.transmartproject.batch.i2b2.database

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Schema-configurable sequence lookup service.
 */
@Component(value = 'sequences')
@JobScope
class I2b2Sequences {

    @Value("#{jobParameters['CRC_SCHEMA']}")
    private String crcSchema

    String getPatient() {
        "${crcSchema}.seq_patient_num"
    }

    String getVisit() {
        "${crcSchema}.seq_encounter_num"
    }
}
