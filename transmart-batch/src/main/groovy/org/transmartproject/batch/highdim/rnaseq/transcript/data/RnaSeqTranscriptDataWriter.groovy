package org.transmartproject.batch.highdim.rnaseq.transcript.data

import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.DatabaseUtil
import org.transmartproject.batch.highdim.platform.annotationsload.AnnotationEntityMap
import org.transmartproject.batch.highdim.rnaseq.data.RnaSeqDataValue

/**
 * Writes the data to de_subject_rnaseq_data.
 */
@Component
@JobScope
class RnaSeqTranscriptDataWriter implements ItemWriter<RnaSeqDataValue> {

    @Autowired
    private AnnotationEntityMap annotationEntityMap

    @Value("#{rnaSeqTranscriptDataJobContextItems.patientIdAssayIdMap}")
    Map<String, Long> patientIdAssayIdMap

    @Value(Tables.RNASEQ_TRANSCRIPT_DATA)
    private SimpleJdbcInsert jdbcInsert

    @Override
    void write(List<? extends RnaSeqDataValue> items) throws Exception {
        int[] result = jdbcInsert.executeBatch(items.collect {

            Long assayId = patientIdAssayIdMap[it.sampleCode]
            if (!assayId) {
                throw new IllegalArgumentException("Passed rnaseq data value with " +
                        "unknown sample code (${it.sampleCode}). Known ids are " +
                        patientIdAssayIdMap.keySet().sort())
            }
            [
                    transcript_id           : annotationEntityMap[it.annotation],
                    assay_id                : assayId,
                    readcount               : it.readCount,
                    normalized_readcount    : it.value,
                    log_normalized_readcount: it.logValue,
                    zscore                  : it.zscore,
            ]
        } as Map[])
        DatabaseUtil.checkUpdateCounts(result,
                "inserting rnaseq data in $Tables.RNASEQ_TRANSCRIPT_DATA")
    }

}
