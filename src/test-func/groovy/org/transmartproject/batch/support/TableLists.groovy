package org.transmartproject.batch.support

import org.transmartproject.batch.clinical.db.objects.Tables

/**
 * While we don't have a backout job.
 */
class TableLists {
    private TableLists() {}

    public static final List<String> CLINICAL_TABLES = [
            Tables.OBSERVATION_FACT,
            Tables.CONCEPT_DIMENSION,
            Tables.PATIENT_TRIAL,
            Tables.PATIENT_DIMENSION,
            Tables.I2B2,
            Tables.I2B2_SECURE,
            Tables.I2B2_TAGS,
            "${Tables.SECURE_OBJECT} CASCADE",
            "${Tables.BIO_EXPERIMENT} CASCADE",
            'biomart.bio_data_uid',
    ]
}
