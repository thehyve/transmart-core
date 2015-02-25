package org.transmartproject.batch.highdim.mrna.data

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.highdim.compute.StandardValuesCalculator
import org.transmartproject.batch.highdim.datastd.StandardDataValue
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntityMap

/**
 * Converts an {@link StandardDataValue} into a row ready for insertion into the
 * database
 */
@Component
@JobScope
class MrnaDataRowConverter {

    @Value("#{jobParameters['STUDY_ID']}")
    private String studyId

    @Autowired
    private AnnotationEntityMap annotationEntityMap

    @Autowired
    StandardValuesCalculator standardValuesCalculator

    /**
     * @see MrnaDataJobContextItems#getPatientIdAssayIdMap()
     */
    @Value("#{mrnaDataJobContextItems.patientIdAssayIdMap}")
    Map<String, Long> patientIdAssayIdMap

    /**
     * @see MrnaDataJobContextItems#getPartitionId()
     */
    @Value('#{mrnaDataJobContextItems.partitionId}')
    Long partitionId

    Map<String, Object> convertMrnaDataValue(StandardDataValue value) {
        Long assayId = patientIdAssayIdMap[value.patient.id]
        if (!assayId) {
            throw new IllegalArgumentException("Passed mrna data value with " +
                    "unknown patient id (${value.patient.id}). Known ids are " +
                    patientIdAssayIdMap.keySet().sort())
        }

        [
                trial_name:    studyId,
                probeset_id:   annotationEntityMap[value.annotation], // validated before
                assay_id:      assayId,
                patient_id:    value.patient.code,
                subject_id:    value.patient.id,
                partition_id:  partitionId,
                *:             standardValuesCalculator.getValueTriplet(value)
        ]
    }
}
