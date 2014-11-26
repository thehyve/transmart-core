package org.transmartproject.batch.clinical.db.objects

/**
 * Constants with the serval used schema-qualified tables.
 *
 * TODO: move this class
 */
final class Tables {
    private Tables() { }

    public static final String CONCEPT_DIMENSION = 'i2b2demodata.concept_dimension'
    public static final String I2B2              = 'i2b2metadata.i2b2'
    public static final String I2B2_SECURE       = 'i2b2metadata.i2b2_secure'
    public static final String PATIENT_TRIAL     = 'i2b2demodata.patient_trial'
    public static final String PATIENT_DIMENSION = 'i2b2demodata.patient_dimension'
    public static final String OBSERVATION_FACT  = 'i2b2demodata.observation_fact'
    public static final String MODIFIER_DIM      = 'i2b2demodata.modifier_dimension'
    public static final String MODIFIER_METADATA = 'i2b2demodata.modifier_metadata'
    public static final String MODIFIER_DIM_VIEW = 'i2b2demodata.modifier_dimension_view'

    public static final String GPL_INFO          = 'deapp.de_gpl_info'
    public static final String MRNA_ANNOTATION   = 'deapp.de_mrna_annotation'
    public static final String SUBJ_SAMPLE_MAP   = 'deapp.de_subject_sample_mapping'
}
