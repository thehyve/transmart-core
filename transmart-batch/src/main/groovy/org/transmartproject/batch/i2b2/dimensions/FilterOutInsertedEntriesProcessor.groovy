package org.transmartproject.batch.i2b2.dimensions

import groovy.util.logging.Slf4j
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import org.transmartproject.batch.beans.JobScopeInterfaced

/**
 * Filter that excludes entries that have already been inserted.
 */
@Component
@JobScopeInterfaced
@Slf4j
class FilterOutInsertedEntriesProcessor
        implements ItemProcessor<DimensionsStoreEntry, DimensionsStoreEntry> {

    @Override
    DimensionsStoreEntry process(DimensionsStoreEntry item) throws Exception {
        if (!item.knownAsExisting) {
            item
        } else if (log.traceEnabled) {
            log.trace("Item $item already exists; filtering it out")
        }
    }
}
