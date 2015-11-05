package org.transmartproject.batch.highdim.proteomics.data

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
class ProteomicsDataRowConverter {

    @Value("#{jobParameters['STUDY_ID']}")
    private String studyId

    @Autowired
    private AnnotationEntityMap annotationEntityMap

    /**
     * @see ProteomicsDataJobContextItems#getPatientIdAssayIdMap()
     */
    @Value("#{proteomicsDataJobContextItems.patientIdAssayIdMap}")
    Map<String, Long> patientIdAssayIdMap

    Map<String, Object> convertProteomicsDataValue(TripleStandardDataValue value) {
        Long assayId = patientIdAssayIdMap[value.sampleCode]
        if (!assayId) {
            throw new IllegalArgumentException("Passed proteomics data value with " +
                    "unknown sample code (${value.sampleCode}). Known ids are " +
                    patientIdAssayIdMap.keySet().sort())
        }

        [
                trial_name           : studyId,
                protein_annotation_id: annotationEntityMap[value.annotation],
                assay_id             : assayId,
                patient_id           : value.patient.code,
                subject_id           : value.patient.id,
                intensity            : value.value,
                log_intensity        : value.logValue,
                zscore               : value.zscore,
        ]
    }
}
