package org.transmart.clinical

/**
 * The Sample domain object represents a physical biological sample.
 * This class resides in the clinical domain where it links to patient
 */
class SampleInfo {

    String id;
    String sampleName;
    String platformName;	// The GEO code for the platform
    String trialName;
    Long assayId;
    String conceptCode;
    Long patientId;

    static mapping = {
        table 'de_subject_sample_mapping'
        version false
        id column: 'sample_id'

        columns {
            // TODO: unify/harmonize column names to indicate that these are ID's
            // TODO: rename these properties (strip name)
            sampleName column:'sample_cd'
            platformName column:'gpl_id'  // TODO: make the platform more generic
            trialName column:'trial_name'
            assayId column:'assay_id'
            conceptCode column:'concept_code'
            patientId column:'patient_id'
        }
    }

    static constraints = {
    }

}