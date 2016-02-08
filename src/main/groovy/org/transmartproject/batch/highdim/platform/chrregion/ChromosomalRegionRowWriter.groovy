package org.transmartproject.batch.highdim.platform.chrregion

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
 * Writes {@link org.transmartproject.batch.highdim.platform.chrregion.ChromosomalRegionRow} objects
 * into the database.
 */
@Component
@StepScope
@Slf4j
class ChromosomalRegionRowWriter implements ItemWriter<ChromosomalRegionRow> {

    @Autowired
    Platform platform

    @Autowired
    SequenceReserver sequenceReserver

    @Value(Tables.CHROMOSOMAL_REGION)
    SimpleJdbcInsert jdbcInsert

    @Override
    void write(List<? extends ChromosomalRegionRow> items) throws Exception {
        List<Map<String, Object>> dbRows = []

        items.each {
            dbRows << doItem(it)
        }

        if (dbRows) {
            jdbcInsert.executeBatch dbRows as Map[]
        }

        log.debug("Written ${dbRows.size()} rows")
    }

    Map<String, Object> doItem(ChromosomalRegionRow row) {
        [
                region_id          : sequenceReserver.getNext(Sequences.CHROMOSOMAL_REGION_ID),
                gpl_id      : platform.id,
                chromosome  : row.chromosome,
                start_bp    : row.startBp,
                end_bp      : row.endBp,
                num_probes  : row.numProbes,
                region_name : row.regionName,
                cytoband    : row.cytoband,
                gene_symbol : row.geneSymbol,
                gene_id     : row.geneId,
                organism    : platform.organism,
        ]
    }
}
