package org.transmartproject.batch.highdim.assays

import groovy.transform.Canonical
import org.transmartproject.batch.concept.ConceptNode
import org.transmartproject.batch.highdim.platform.Platform
import org.transmartproject.batch.patient.Patient

/**
 * Represents a row in de_subject_sample_mapping.
 */
@Canonical
class Assay {

    Long id
    Patient patient
    ConceptNode concept
    String studyId
    String sampleType
    String sampleCode
    String tissueType
    String timePoint
    Platform platform

    Map<String, Object> toDatabaseRow() {
        [
                patient_id  : patient.code,
                // site_id irrelevant
                subject_id  : patient.id,
                // subject_type irrelevant
                concept_code: concept.code,
                assay_id    : id,
                // patient_uid irrelevant
                sample_type : sampleType,
                // assay_uid irrelevant
                trial_name  : studyId,
                timepoint   : timePoint,
                // timepoint_cd irrelevant
                // sample_type_cd irelevant
                // tissue_type_cd irrelevant
                // platform irrelevant
                // platform_cd irrelevant
                tissue_type : tissueType,
                // data_uid irrelevant
                gpl_id      : platform.id,
                // rbm_panel irrelevant
                // sample_id irrelevant
                sample_cd   : sampleCode,
                // category_cd irrelevant
                // source_cd irrelevant
                // omic_source_study irrelevant
                // omic_patient_num irrelevant
                // omic_patient_id irrelevant
                //partition_id irrelevant
        ]
    }

    String getSampleType() {
        this.@sampleType ?: 'Unknown'
    }
}
