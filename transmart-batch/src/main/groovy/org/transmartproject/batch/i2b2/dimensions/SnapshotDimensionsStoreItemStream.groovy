package org.transmartproject.batch.i2b2.dimensions

import groovy.util.logging.Slf4j
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStream
import org.springframework.batch.item.ItemStreamException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Puts the {@link DimensionsStore} in the step execution context and restores
 * it if necessary.
 */
@Component
@JobScope
@Slf4j
class SnapshotDimensionsStoreItemStream implements ItemStream {

    private final static String DIMENSIONS_STORE_KEY = 'dimensionsStore'

    @Autowired
    private DimensionsStore dimensionsStore

    @Override
    void open(ExecutionContext executionContext) throws ItemStreamException {
        DimensionsStore storedStore =
                executionContext.get(DIMENSIONS_STORE_KEY)
        if (storedStore) {
            log.info("Restored list of dimensions from previous run")
            dimensionsStore.restore(storedStore)
        }
    }

    @Override
    void update(ExecutionContext executionContext) throws ItemStreamException {
        log.debug("Storing list of dimensions in the step context")
        def storedStore = new DimensionsStore()
        storedStore.restore(dimensionsStore)
        executionContext.put(DIMENSIONS_STORE_KEY, storedStore)
    }

    @Override
    @SuppressWarnings('CloseWithoutCloseable')
    void close() throws ItemStreamException {}
}
