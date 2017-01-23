package org.transmartproject.batch.highdim.mrna.platform

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
 * Writes {@link MrnaAnnotationRow} objects into the database.
 */
@Component
@StepScope
@Slf4j
class MrnaAnnotationWriter implements ItemWriter<MrnaAnnotationRow> {

    @Autowired
    Platform platform

    @Autowired
    SequenceReserver sequenceReserver

    @Value(Tables.MRNA_ANNOTATION)
    SimpleJdbcInsert jdbcInsert

    @Override
    void write(List<? extends MrnaAnnotationRow> items) throws Exception {
        List<Map<String, Object>> dbRows = []

        items.each {
            dbRows += doItem it
        }

        if (dbRows) {
            jdbcInsert.executeBatch dbRows as Map[]
        }

        log.debug("Written ${dbRows.size()} rows")
    }

    List<Map<String, Object>> doItem(MrnaAnnotationRow row) {
        List out = Lists.newLinkedList()
        long probesetId = sequenceReserver.getNext(Sequences.PROBESET_ID)

        row.geneList.size().times { i ->
            out << [
                    gpl_id     : platform.id,
                    probe_id   : row.probeName,
                    probeset_id: probesetId,
                    gene_symbol: row.geneList[i],
                    gene_id    : row.entrezIdList[i],
                    organism   : platform.organism]
        }
        out
    }
}
