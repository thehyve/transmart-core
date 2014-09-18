package org.transmartproject.batch.support

/**
 *
 */
class DatabaseObject {

    static class Schema {
        static final String I2B2METADATA = 'i2b2metadata'
        static final String I2B2DEMODATA = 'i2b2demodata'

    }

    static class Sequence {
        static final String PATIENT = 'i2b2demodata.SEQ_PATIENT_NUM'
        static final String CONCEPT = 'i2b2metadata.I2B2_ID_SEQ'
        static final String I2B2_RECORDID = 'i2b2metadata.i2b2_record_id_seq'
    }

    static void checkUpdateCounts(int[] counts, String operation) {
        if (!counts.every { it == 1 }) {
            throw new RuntimeException("Updated rows mismatch while $operation")
        }
    }

}
