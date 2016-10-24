package org.transmartproject.batch.highdim.platform.chrregion.transcript

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.transmartproject.batch.clinical.db.objects.Sequences
import org.transmartproject.batch.clinical.db.objects.Tables
import org.transmartproject.batch.db.SequenceReserver
import org.transmartproject.batch.highdim.platform.Platform

/**
 * Writes RnaSeq transcript data into the database.
 */
@Component
@StepScope
@Slf4j
class RnaSeqTranscriptAnnotationWriter implements ItemWriter<RnaSeqTranscriptAnnotationRow> {

    @Autowired
    Platform platform

    @Autowired
    SequenceReserver sequenceReserver

    @Value(Tables.RNASEQ_TRANSCRIPT_ANNOTATION)
    SimpleJdbcInsert jdbcInsert

    @Override
    void write(List<? extends RnaSeqTranscriptAnnotationRow> items) throws Exception {
        List<Map<String, Object>> dbRows = []

        items.each {
            dbRows << doItem(it)
        }

        if (dbRows) {
            jdbcInsert.executeBatch dbRows as Map[]
        }

        log.debug("Written ${dbRows.size()} rows")
    }

    Map<String, Object> doItem(RnaSeqTranscriptAnnotationRow row) {
        [
                id        : sequenceReserver.getNext(Sequences.CHROMOSOMAL_REGION_ID),
                gpl_id    : platform.id,
                chromosome: row.chromosome,
                start     : row.start,
                end       : row.end,
                transcript: row.transcript
        ]
    }
}
