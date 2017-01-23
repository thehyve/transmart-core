package org.transmartproject.batch.highdim.mirna.data

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
class MirnaDataRowConverter {

    @Value("#{jobParameters['STUDY_ID']}")
    private String studyId

    @Autowired
    private AnnotationEntityMap annotationEntityMap

    /**
     * @see MirnaDataJobContextItems#getSampleCodeAssayIdMap()
     */
    @Value("#{mirnaDataJobContextItems.sampleCodeAssayIdMap}")
    Map<String, Long> sampleCodeAssayIdMap

    Map<String, Object> convertMirnaDataValue(TripleStandardDataValue value) {
        Long assayId = sampleCodeAssayIdMap[value.sampleCode]
        if (!assayId) {
            throw new IllegalArgumentException("Passed mirna data value with " +
                    "unknown sample code (${value.sampleCode}). Known codes are " +
                    sampleCodeAssayIdMap.keySet().sort())
        }

        [
                trial_name   : studyId,
                probeset_id  : annotationEntityMap[value.annotation],
                assay_id     : assayId,
                patient_id   : value.patient.code,
                raw_intensity: value.value,
                log_intensity: value.logValue,
                zscore       : value.zscore,
        ]
    }
}
