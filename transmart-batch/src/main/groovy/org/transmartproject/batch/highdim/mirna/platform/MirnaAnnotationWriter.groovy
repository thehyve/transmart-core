package org.transmartproject.batch.highdim.mirna.platform

import com.google.common.collect.Lists
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
 * Writes {@link MirnaAnnotationRow} objects into the database.
 */
@Component
@StepScope
@Slf4j
class MirnaAnnotationWriter implements ItemWriter<MirnaAnnotationRow> {

    @Autowired
    Platform platform

    @Autowired
    SequenceReserver sequenceReserver

    @Value(Tables.MIRNA_ANNOTATION)
    SimpleJdbcInsert jdbcInsert

    @Override
    void write(List<? extends MirnaAnnotationRow> items) throws Exception {
        List<Map<String, Object>> dbRows = []

        items.each {
            dbRows += doItem it
        }

        if (dbRows) {
            jdbcInsert.executeBatch dbRows as Map[]
        }

        log.debug("Written ${dbRows.size()} rows")
    }

    List<Map<String, Object>> doItem(MirnaAnnotationRow row) {
        List out = Lists.newLinkedList()
        long probesetId = sequenceReserver.getNext(Sequences.PROBESET_ID)

        out << [
                probeset_id: probesetId,
                id_ref     : row.idRef,
                mirna_id   : row.mirnaId,
                gpl_id     : platform.id,
                organism   : platform.organism]
        out
    }
}
