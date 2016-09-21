package org.transmartproject.batch.highdim.metabolomics.data

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.highdim.assays.SaveAssayIdListener
import org.transmartproject.batch.highdim.datastd.StandardDataValue
import org.transmartproject.batch.highdim.datastd.TripleStandardDataValue
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntityMap

/**
 * Converts an {@link StandardDataValue} into a row ready for insertion into the
 * {@link Tables#METAB_DATA} table.
 */
@Component
@JobScope
class MetabolomicsDataConverter {

    @Value("#{jobParameters['STUDY_ID']}")
    private String studyId

    @Autowired
    private AnnotationEntityMap annotationEntityMap

    /**
     * @see SaveAssayIdListener#MAPPINGS_CONTEXT_KEY
     */
    @Value("#{jobExecutionContext['sampleCodeAssayIdMap']}")
    Map<String, Long> sampleCodeAssayIdMap

    Map<String, Object> convertMetabolomicsDataValue(TripleStandardDataValue value) {
        Long assayId = sampleCodeAssayIdMap[value.sampleCode]
        assert assayId != null // should have been validated before

        Long metaboliteId = annotationEntityMap[value.annotation]
        assert metaboliteId != null // should have been validated before

        [
                trial_name              : studyId,
                metabolite_annotation_id: metaboliteId,
                assay_id                : assayId,
                patient_id              : value.patient.code,
                subject_id              : value.patient.id,
                raw_intensity           : value.value,
                log_intensity           : value.logValue,
                zscore                  : value.zscore,
        ]
    }

}
