package org.transmartproject.batch.i2b2.dimensions

import org.springframework.batch.item.ItemReader

/**
 * Reads external ids for dimension objects of a certain type from
 * {@link DimensionsStore}.
 */
class DimensionObjectExternalIdReader implements ItemReader<String> {

    String dimensionKey /* to be configured */

    DimensionsStore dimensionsStore /* to be configured */

    @Lazy
    @SuppressWarnings('PrivateFieldCouldBeFinal')
    private Iterator iterator = fetchIterator()

    private Iterator fetchIterator() {
        dimensionsStore.getExternalIdIteratorForDimensionKey(dimensionKey)
    }

    @Override
    String read() {
        if (iterator.hasNext()) {
            iterator.next()
        } // else null
    }
}
