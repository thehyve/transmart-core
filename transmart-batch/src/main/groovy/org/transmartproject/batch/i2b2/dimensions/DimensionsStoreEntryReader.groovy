package org.transmartproject.batch.i2b2.dimensions

import groovy.transform.CompileStatic
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader

/**
 * Exposes the store content through a streamable reader of
 * {@link DimensionsStoreEntry} objects.
 */
@CompileStatic
class DimensionsStoreEntryReader extends AbstractItemCountingItemStreamItemReader<DimensionsStoreEntry> {

    DimensionsStore dimensionsStore

    String dimensionKey

    private Iterator<DimensionsStoreEntry> iterator

    @Override
    protected DimensionsStoreEntry doRead() throws Exception {
        if (iterator.hasNext()) {
            iterator.next()
        }
    }

    @Override
    protected void doOpen() throws Exception {
        iterator = dimensionsStore.getEntriesForDimensionKey(dimensionKey)
    }

    @Override
    protected void doClose() throws Exception {
        iterator = null
    }
}
