package org.transmartproject.batch.i2b2.database

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Schema-configurable table lookup service.
 */
@Component(value = 'tables')
@JobScope
class I2b2Tables {

    @Value("#{jobParameters['CRC_SCHEMA']}")
    private String crcSchema

    String getCrcSchema() {
        crcSchema
    }

    String getConceptDimension() {
        "${crcSchema}.concept_dimension"
    }

    String getPatientDimension() {
        "${crcSchema}.patient_dimension"
    }

    String getPatientMapping() {
        "${crcSchema}.patient_mapping"
    }

    String getVisitDimension() {
        "${crcSchema}.visit_dimension"
    }

    String getEncounterMapping() {
        "${crcSchema}.encounter_mapping"
    }

    String getProviderDimension() {
        "${crcSchema}.provider_dimension"
    }

    String getCodeLookup() {
        "${crcSchema}.code_lookup"
    }

    String getObservationFact() {
        "${crcSchema}.observation_fact"
    }

    String getModifierDimension() {
        "${crcSchema}.modifier_dimension"
    }
}
