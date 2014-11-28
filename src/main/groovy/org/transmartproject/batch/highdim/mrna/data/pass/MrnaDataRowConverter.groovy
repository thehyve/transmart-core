package org.transmartproject.batch.highdim.mrna.data.pass

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.highdim.mrna.data.MrnaDataJobContextItems
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntityMap

/**
 * Converts an {@link MrnaDataValue} into a row ready for insertion into the
 * database
 */
@Component
@JobScope
class MrnaDataRowConverter {

    private static final int LOG_BASE = 2

    @Value("#{jobParameters['STUDY_ID']}")
    private String studyId

    @Autowired
    private AnnotationEntityMap annotationEntityMap

    @Autowired
    private MrnaDataJobContextItems mrnaDataJobConfiguration

    @Lazy
    Map<String, Long> patientIdAssayIdMap = mrnaDataJobConfiguration.patientIdAssayIdMap

    @Lazy
    Long partitionId = mrnaDataJobConfiguration.partitionId

    @Lazy
    double mean = mrnaDataJobConfiguration.calculatedMean

    @Lazy
    double stdDev = Math.sqrt(mrnaDataJobConfiguration.calculatedVariance)

    private double clamp(double lowerBound, double upperBound, double value) {
        Math.min(upperBound, Math.max(lowerBound, value))
    }

    Map<String, Object> convertMrnaDataValue(MrnaDataValue value) {
        Long assayId = patientIdAssayIdMap[value.patient.id]
        if (!assayId) {
            throw new IllegalArgumentException("Passed mrna data value with " +
                    "unknown patient id (${value.patient.id}). Known ids are " +
                    patientIdAssayIdMap.keySet().sort())
        }

        [
                trial_name:    studyId,
                probeset_id:   annotationEntityMap[value.probe], // validated before
                assay_id:      assayId,
                patient_id:    value.patient.code,
                subject_id:    value.patient.id,
                raw_intensity: value.value,
                log_intensity: Math.log(value.value)/Math.log(LOG_BASE),
                zscore:        clamp(-2.5, 2.5, (value.value - mean)/stdDev),
                partition_id:  partitionId,
        ]
    }

}
