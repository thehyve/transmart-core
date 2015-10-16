package org.transmartproject.batch.highdim.mrna.data

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.highdim.datastd.TripleStandardDataValue
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntityMap

/**
 * Converts an {@link TripleStandardDataValue} into a row ready for insertion into the
 * database
 */
@Component
@JobScope
class MrnaDataRowConverter {

    @Value("#{jobParameters['STUDY_ID']}")
    private String studyId

    @Autowired
    private AnnotationEntityMap annotationEntityMap

    /**
     * @see MrnaDataJobContextItems#getSampleCodeAssayIdMap()
     */
    @Value("#{mrnaDataJobContextItems.sampleCodeAssayIdMap}")
    Map<String, Long> sampleCodeAssayIdMap

    Map<String, Object> convertMrnaDataValue(TripleStandardDataValue value) {
        Long assayId = sampleCodeAssayIdMap[value.sampleCode]
        if (!assayId) {
            throw new IllegalArgumentException("Passed mrna data value with " +
                    "unknown sample code (${value.sampleCode}). Known codes are " +
                    sampleCodeAssayIdMap.keySet().sort())
        }

        [
                trial_name   : studyId,
                probeset_id  : annotationEntityMap[value.annotation], // validated before
                assay_id     : assayId,
                patient_id   : value.patient.code,
                subject_id   : value.patient.id,
                raw_intensity: value.value,
                log_intensity: value.logValue,
                zscore       : value.zscore,
        ]
    }
}
