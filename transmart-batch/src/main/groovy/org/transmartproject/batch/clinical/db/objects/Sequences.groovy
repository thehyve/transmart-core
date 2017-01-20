package org.transmartproject.batch.clinical.db.objects

/**
 * TODO: move this class
 */
final class Sequences {
    private Sequences() {}

    public static final String PATIENT = 'i2b2demodata.seq_patient_num'
    public static final String VISIT = 'i2b2demodata.seq_encounter_num'
    public static final String CONCEPT = 'i2b2demodata.concept_id'
    public static final String TRIAL_VISIT = 'i2b2demodata.trial_visit_num_seq'
    public static final String STUDY = 'i2b2demodata.study_num_seq'
    public static final String DIMENSION_DESCRIPTION = 'i2b2metadata.dimension_description_id_seq'

    public static final String BIO_DATA_ID = 'biomart.seq_bio_data_id'

    public static final String SEARCH_SEQ_DATA_ID = 'searchapp.seq_search_data_id'

    public static final String PROBESET_ID = 'tm_cz.seq_probeset_id'

    public static final String ASSAY_ID = 'deapp.seq_assay_id'
    public static final String MRNA_PARTITION_ID = 'deapp.seq_mrna_partition_id'

    public static final String METAB_SUB_PATHWAY_ID = 'deapp.metabolite_sub_pth_id'
    public static final String METAB_SUPER_PATHWAY_ID = 'deapp.metabolite_sup_pth_id'
    public static final String METAB_ANNOT_ID = 'deapp.metabolomics_annot_id'

    public static final String PROTEOMICS_ANNOTATION_ID = 'deapp.protein_annotation_id'
    public static final String PROTEOMICS_PARTITION_ID = 'deapp.seq_proteomics_partition_id'

    public static final String CHROMOSOMAL_REGION_ID = 'deapp.de_chromo_region_id_seq'

    public static final String RNASEQ_PARTITION_ID = 'deapp.seq_rna_partition_id'

}
