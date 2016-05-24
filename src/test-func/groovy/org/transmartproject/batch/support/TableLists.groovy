package org.transmartproject.batch.support

import com.sun.org.apache.xpath.internal.operations.String
import org.transmartproject.batch.clinical.db.objects.Tables

/**
 * While we don't have a backout job.
 */
class TableLists {
    private TableLists() {}

    public static final List<String> CLINICAL_TABLES = [
            Tables.OBSERVATION_FACT,
            Tables.CONCEPT_COUNTS,
            Tables.CONCEPT_DIMENSION,
            Tables.CONCEPT_COUNTS,
            Tables.PATIENT_TRIAL,
            Tables.PATIENT_DIMENSION,
            Tables.TABLE_ACCESS,
            Tables.I2B2_TAGS,
            Tables.I2B2_SECURE,
            Tables.I2B2,
            Tables.SECURE_OBJECT,
            Tables.BIO_EXPERIMENT,
            Tables.BIO_DATA_UID,
    ]

    public static final List<String> I2B2_TABLES = [
            Tables.OBSERVATION_FACT,
            Tables.CONCEPT_DIMENSION,
            Tables.PATIENT_DIMENSION,
            Tables.PATIENT_MAPPING,
            Tables.VISIT_DIMENSION,
            Tables.ENCOUNTER_MAPPING,
            Tables.PROV_DIMENSION,
     ]

    public static final List<String> METABOLOMICS_TABLES = [
            Tables.GPL_INFO,
            Tables.SUBJ_SAMPLE_MAP,
            Tables.METAB_ANNOT_SUB,
            Tables.METAB_ANNOTATION,
            Tables.METAB_SUB_PATH,
            Tables.METAB_SUPER_PATH,
            Tables.METAB_DATA
    ]

    public static final List<String> MRNA_TABLES = [
            Tables.GPL_INFO,
            Tables.SUBJ_SAMPLE_MAP,
            Tables.MRNA_DATA,
            Tables.MRNA_ANNOTATION,
    ]

    public static final List<String> PROTEOMICS_TABLES = [
            Tables.GPL_INFO,
            Tables.SUBJ_SAMPLE_MAP,
            Tables.PROTEOMICS_DATA,
            Tables.PROTEOMICS_ANNOTATION,
    ]

    public static final List<String> GWAS_TABLE_LISTS = [
            Tables.BIO_ASSAY_ANALYSIS,
            Tables.BIO_ASSAY_ANALYSIS_EXT,
            Tables.BIO_DATA_UID,
            Tables.BIO_ASSAY_ANALYSIS_GWAS,
            Tables.BIO_ASY_ANAL_GWAS_TOP500,
    ]

    public static final List<String> RNA_SEQ_TABLES = [
            Tables.GPL_INFO,
            Tables.SUBJ_SAMPLE_MAP,
            Tables.RNASEQ_DATA,
            Tables.CHROMOSOMAL_REGION,
    ]

    public static final List<String> CNV_TABLES = [
            Tables.GPL_INFO,
            Tables.SUBJ_SAMPLE_MAP,
            Tables.CNV_DATA,
            Tables.CHROMOSOMAL_REGION,
    ]

    public static final List<String> MIRNA_TABLES = [
            Tables.GPL_INFO,
            Tables.SUBJ_SAMPLE_MAP,
            Tables.MIRNA_DATA,
            Tables.MIRNA_ANNOTATION,
    ]

}
