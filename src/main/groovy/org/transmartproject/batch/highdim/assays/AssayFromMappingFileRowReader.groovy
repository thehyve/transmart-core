package org.transmartproject.batch.highdim.assays

import groovy.util.logging.Slf4j
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced

/**
 * Reads {@link Assay}s from {@link MappingsFileRowStore}. Transforms the
 * {@link MappingFileRow}s into {@link Assay}s
 */
@Component
@JobScopeInterfaced
@Slf4j
class AssayFromMappingFileRowReader extends AbstractItemCountingItemStreamItemReader<Assay> {

    @Autowired
    private MappingsFileRowStore mappingsFileRowStore

    @Autowired
    private AssayFactory assayFactory

    AssayFromMappingFileRowReader() {
        name = getClass().simpleName
    }

    @Override
    protected Assay doRead() throws Exception {
        int pos = currentItemCount
        def currentRow = mappingsFileRowStore.rows[pos - 1]
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
        maxItemCount = mappingsFileRowStore.rows.size()
    }

    @Override
    protected void doClose() throws Exception {}
}
