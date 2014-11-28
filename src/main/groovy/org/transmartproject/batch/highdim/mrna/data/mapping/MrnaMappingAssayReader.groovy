package org.transmartproject.batch.highdim.mrna.data.mapping

import groovy.util.logging.Slf4j
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced
import org.transmartproject.batch.highdim.assays.Assay
import org.transmartproject.batch.highdim.assays.AssayFactory

/**
 * Reads {@link Assay}s from {@link MrnaMappings}.
 */
@Component
@JobScopeInterfaced
@Slf4j
class MrnaMappingAssayReader extends AbstractItemCountingItemStreamItemReader<Assay> {
    @Autowired
    private MrnaMappings mrnaMappings

    @Autowired
    private AssayFactory assayFactory

    MrnaMappingAssayReader() {
        name = getClass().simpleName
    }

    @Override
    protected Assay doRead() throws Exception {
        int pos = currentItemCount
        def currentRow = mrnaMappings.rows[pos - 1]
        Assay assay = assayFactory.createFromMappingRow(currentRow)
        log.debug("Will return assay $assay for position $pos")

        assay
    }

    @Override
    protected void jumpToItem(int itemIndex) throws Exception {
        currentItemCount = itemIndex
    }

    @Override
    protected void doOpen() throws Exception {
        maxItemCount = mrnaMappings.rows.size()
    }

    @Override
    protected void doClose() throws Exception {}
}
