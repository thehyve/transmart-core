package org.transmartproject.batch.clinical.db.objects

/**
 * Constants with the several used schema-qualified tables.
 *
 * TODO: move this class
 */
final class Tables {
    private Tables() {}

    static String tableName(String qualifiedTableName) {
        qualifiedTableName.split(/\./, 2)[1]
    }

    static String schemaName(String qualifiedTableName) {
        qualifiedTableName.split(/\./, 2)[0]
    }

    public static final String CONCEPT_DIMENSION        = 'i2b2demodata.concept_dimension'
    public static final String CONCEPT_COUNTS           = 'i2b2demodata.concept_counts'
    public static final String TABLE_ACCESS             = 'i2b2metadata.table_access'
    public static final String I2B2                     = 'i2b2metadata.i2b2'
    public static final String I2B2_SECURE              = 'i2b2metadata.i2b2_secure'
    public static final String I2B2_TAGS                = 'i2b2metadata.i2b2_tags'
    public static final String PATIENT_TRIAL            = 'i2b2demodata.patient_trial'
    public static final String PATIENT_DIMENSION        = 'i2b2demodata.patient_dimension'
    public static final String PATIENT_MAPPING          = 'i2b2demodata.patient_mapping'
    public static final String VISIT_DIMENSION          = 'i2b2demodata.visit_dimension'
    public static final String ENCOUNTER_MAPPING        = 'i2b2demodata.encounter_mapping'
    public static final String PROV_DIMENSION           = 'i2b2demodata.provider_dimension'
    public static final String CODE_LOOKUP              = 'i2b2demodata.code_lookup'
    public static final String OBSERVATION_FACT         = 'i2b2demodata.observation_fact'
    public static final String MODIFIER_DIM             = 'i2b2demodata.modifier_dimension'
    public static final String MODIFIER_METADATA        = 'i2b2demodata.modifier_metadata'
    public static final String MODIFIER_DIM_VIEW        = 'i2b2demodata.modifier_dimension_view'
    public static final String BIO_EXPERIMENT           = 'biomart.bio_experiment'
    public static final String SECURE_OBJECT            = 'searchapp.search_secure_object'
    public static final String SEARCH_AUTH_SEC_OBJ_ACC  = 'searchapp.search_auth_sec_object_access'
    public static final String BIO_DATA_UID             = 'biomart.bio_data_uid'

    public static final String GPL_INFO                 = 'deapp.de_gpl_info'
    public static final String SUBJ_SAMPLE_MAP          = 'deapp.de_subject_sample_mapping'

    public static final String MRNA_ANNOTATION          = 'deapp.de_mrna_annotation'
    public static final String MRNA_DATA                = 'deapp.de_subject_microarray_data'

    public static final String METAB_ANNOTATION         = 'deapp.de_metabolite_annotation'
    public static final String METAB_SUB_PATH           = 'deapp.de_metabolite_sub_pathways'
    public static final String METAB_SUPER_PATH         = 'deapp.de_metabolite_super_pathways'
    public static final String METAB_ANNOT_SUB          = 'deapp.de_metabolite_sub_pway_metab'
    public static final String METAB_DATA               = 'deapp.de_subject_metabolomics_data'

    public static final String PROTEOMICS_ANNOTATION    = 'deapp.de_protein_annotation'
    public static final String PROTEOMICS_DATA          = 'deapp.de_subject_protein_data'

    public static final String RC_SNP_INFO              = 'deapp.de_rc_snp_info'
    public static final String BIO_ASSAY_ANALYSIS       = 'biomart.bio_assay_analysis'
    public static final String BIO_ASSAY_ANALYSIS_EXT   = 'biomart.bio_assay_analysis_ext'
    public static final String BIO_ASSAY_ANALYSIS_GWAS  = 'biomart.bio_assay_analysis_gwas'
    public static final String BIO_ASY_ANAL_GWAS_TOP500 = 'biomart.bio_asy_analysis_gwas_top50'
    public static final String BIO_MARKER               = 'biomart.bio_marker'

    public static final String CHROMOSOMAL_REGION       = 'deapp.de_chromosomal_region'
    public static final String RNASEQ_DATA              = 'deapp.de_subject_rnaseq_data'

    public static final String CNV_DATA                 = 'deapp.de_subject_acgh_data'

    public static final String MIRNA_ANNOTATION         = 'deapp.de_qpcr_mirna_annotation'
    public static final String MIRNA_DATA               = 'deapp.de_subject_mirna_data'

}
