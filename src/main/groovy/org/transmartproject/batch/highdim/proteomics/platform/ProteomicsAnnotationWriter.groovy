package org.transmartproject.batch.highdim.proteomics.platform

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
 * Writes {@link org.transmartproject.batch.highdim.proteomics.platform.ProteomicsAnnotationRow} objects
 * into the database.
 */
@Component
@StepScope
@Slf4j
class ProteomicsAnnotationWriter implements ItemWriter<ProteomicsAnnotationRow> {

    @Autowired
    Platform platform

    @Autowired
    SequenceReserver sequenceReserver

    @Value(Tables.PROTEOMICS_ANNOTATION)
    SimpleJdbcInsert jdbcInsert

    @Override
    void write(List<? extends ProteomicsAnnotationRow> items) throws Exception {
        List<Map<String, Object>> dbRows = []

        items.each {
            dbRows << doItem(it)
        }

        if (dbRows) {
            jdbcInsert.executeBatch dbRows as Map[]
        }

        log.debug("Written ${dbRows.size()} rows")
    }

    Map<String, Object> doItem(ProteomicsAnnotationRow row) {
        [
                id           : sequenceReserver.getNext(Sequences.PROTEOMICS_ANNOTATION_ID),
                gpl_id       : platform.id,
                peptide      : row.probesetId,
                uniprot_id   : row.uniprotId,
                uniprot_name : row.uniprotName,
                organism     : platform.organism,
                chromosome   : row.chromosome,
                start_bp     : row.startBp,
                end_bp       : row.endBp
        ]
    }
}
