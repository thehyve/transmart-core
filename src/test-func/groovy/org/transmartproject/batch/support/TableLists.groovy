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
            Tables.CONCEPT_DIMENSION,
            Tables.CONCEPT_COUNTS,
            Tables.PATIENT_TRIAL,
            Tables.PATIENT_DIMENSION,
            Tables.I2B2,
            Tables.I2B2_SECURE,
            Tables.I2B2_TAGS,
            "${Tables.SECURE_OBJECT} CASCADE",
            "${Tables.BIO_EXPERIMENT} CASCADE",
            'biomart.bio_data_uid',
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
            "${Tables.GPL_INFO} CASCADE",
            Tables.SUBJ_SAMPLE_MAP,
            "${Tables.METAB_ANNOT_SUB}, " +
                    "${Tables.METAB_ANNOTATION}, " +
                    "${Tables.METAB_SUB_PATH}, " +
                    "${Tables.METAB_SUPER_PATH}, " +
                    "${Tables.METAB_DATA}",
    ]

    public static final List<String> MRNA_TABLES = [
            "${Tables.GPL_INFO} CASCADE",
            Tables.SUBJ_SAMPLE_MAP,
            Tables.MRNA_DATA,
            Tables.MRNA_ANNOTATION,
    ]
}
