package org.transmartproject.batch.highdim.datastd

/**
 * Process row of items
 */
interface RowItemsProcessor<T> {

    List<T> process(List<T> rowItems)

}
