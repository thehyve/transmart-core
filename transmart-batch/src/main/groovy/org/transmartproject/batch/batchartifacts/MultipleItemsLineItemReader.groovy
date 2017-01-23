package org.transmartproject.batch.batchartifacts

import com.google.common.collect.Lists
import groovy.util.logging.Slf4j
import org.springframework.batch.item.*
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader
import org.springframework.beans.factory.InitializingBean
import org.springframework.util.Assert
import org.springframework.util.ClassUtils
import org.transmartproject.batch.highdim.datastd.RowItemsProcessor

/**
 * Reads wide file format where each row contains normally more than one item.
 */
@Slf4j
class MultipleItemsLineItemReader<T> extends ItemStreamSupport
        implements ItemStreamReader<T>, InitializingBean, Closeable {

    {
        setName(ClassUtils.getShortName(getClass()))
    }

    /**
     * Maps a field set to the multiple items (required)
     */
    MultipleItemsFieldSetMapper<T> multipleItemsFieldSetMapper

    ItemStreamReader<FieldSet> itemStreamReader

    RowItemsProcessor<T> rowItemsProcessor

    private static final String SAVED_ITEM_POS_IN_CACHE = 'savedItemPositionInTheCache'
    private final List<T> cache = Lists.<T> newLinkedList()
    private int savedItemPositionInTheCache = 0

    @Override
    T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!hasNextCacheItem()) {
            renewCacheWithNextRowItems()
        }
        if (hasNextCacheItem()) {
            nextCacheItem()
        }
    }

    @Override
    void afterPropertiesSet() throws Exception {
        Assert.notNull(multipleItemsFieldSetMapper, 'mapper has to be specified')
        Assert.notNull(itemStreamReader, 'item reader has to be specified')
    }

    @Override
    void open(ExecutionContext executionContext) throws ItemStreamException {
        if (executionContext.containsKey(
                getExecutionContextKey(SAVED_ITEM_POS_IN_CACHE))) {
            savedItemPositionInTheCache = executionContext.getInt(
                    getExecutionContextKey(SAVED_ITEM_POS_IN_CACHE))
            log.debug("Load the item position (${savedItemPositionInTheCache}) from the context.")
        }
        //Dirty hack!
        boolean refillBuffer = false
        if (itemStreamReader instanceof AbstractItemCountingItemStreamItemReader) {
            String readCountKey = itemStreamReader
                    .getExecutionContextKey(AbstractItemCountingItemStreamItemReader.READ_COUNT)
            if (executionContext.containsKey(readCountKey)) {
                int lineCount = executionContext.getInt(readCountKey)
                if (lineCount > 0) {
                    refillBuffer = true
                    lineCount--
                    executionContext.putInt(readCountKey, lineCount)
                    log.debug("Reset line count back to ${lineCount} to refill the buffer.")
                }
            }
        }
        itemStreamReader.open(executionContext)
        if (refillBuffer) {
            addNextRowItemsToCache()
        }
    }

    @Override
    void update(ExecutionContext executionContext) throws ItemStreamException {
        log.debug("Save the item position (${savedItemPositionInTheCache}) to the context.")
        executionContext.put(
                getExecutionContextKey(SAVED_ITEM_POS_IN_CACHE), savedItemPositionInTheCache)
        itemStreamReader.update(executionContext)
    }

    @Override
    void close() throws ItemStreamException {
        cache.clear()
        savedItemPositionInTheCache = 0
        itemStreamReader.close()
    }

    private renewCacheWithNextRowItems() {
        savedItemPositionInTheCache = 0
        cache.clear()
        addNextRowItemsToCache()
    }

    private addNextRowItemsToCache() {
        def (endOfRows, rowItems) = readRowItems()
        while (!(endOfRows || rowItems)) {
            (endOfRows, rowItems) = readRowItems()
        }

        if (rowItems) {
            cache.addAll(rowItems)
        }
    }

    private List readRowItems() {
        FieldSet fieldSet = itemStreamReader.read()
        if (fieldSet == null) {
            return [true]
        }
        List<T> rowItems = multipleItemsFieldSetMapper.mapFieldSet(fieldSet)
        if (rowItemsProcessor) {
            rowItems = rowItemsProcessor.process(rowItems)
        }
        [false, rowItems]
    }

    private T nextCacheItem() {
        cache.get(savedItemPositionInTheCache++)
    }

    private boolean hasNextCacheItem() {
        cache.size() > savedItemPositionInTheCache
    }
}
