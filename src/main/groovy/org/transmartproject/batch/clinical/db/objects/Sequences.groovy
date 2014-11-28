package org.transmartproject.batch.clinical.db.objects

/**
 * TODO: move this class
 */
final class Sequences {
    private Sequences() {}

    static final String PATIENT = 'i2b2demodata.SEQ_PATIENT_NUM'
    static final String CONCEPT = 'i2b2metadata.I2B2_ID_SEQ'
    static final String I2B2_RECORDID = 'i2b2metadata.i2b2_record_id_seq'

    static final String PROBESET_ID = 'tm_cz.SEQ_PROBESET_ID'

    static final String ASSAY_ID = 'deapp.seq_assay_id'

    static final String MRNA_PARTITION_ID = 'deapp.seq_mrna_partition_id'
}
