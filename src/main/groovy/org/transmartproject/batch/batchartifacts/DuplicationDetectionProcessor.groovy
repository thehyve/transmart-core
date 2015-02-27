package org.transmartproject.batch.batchartifacts

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import groovy.util.logging.Slf4j
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemStreamException
import org.springframework.batch.item.ItemStreamSupport
import org.springframework.batch.item.validator.ValidationException

/**
 * Processor that detects and either errors or just warns in case there is a
 * duplicate item.
 */
@Slf4j
class DuplicationDetectionProcessor<T> extends ItemStreamSupport implements ItemProcessor<T, T> {

    private static final String READ_COUNT = "read.count"
    private static final String KEYS_MAP = "keys.map"

    /* user settable properties */
    boolean saveState = true

    boolean throwOnRepeated = true

    boolean ignoreNullKeys = true

    Closure calculateKey = Closure.IDENTITY

    /* private fields */
    private int currentItemCount = 0

    private Map<Object, Long> seen = [:]

    DuplicationDetectionProcessor() {
        name = getClass().name
    }

    @Override
    T process(T item) throws Exception {
        def key = calculateKey item
        currentItemCount++

        if (ignoreNullKeys && key == null) {
            return item
        }

        if (seen.containsKey(key)) {
            seenBefore(item, key, seen[key])
        } else {
            seen[key] = currentItemCount
        }

        item /* return original item */
    }

    private void seenBefore(T item, Object key, Long previousLine) {
        def message = "Item ${item} (key '$key') on line $currentItemCount " +
                "was first seen on line $previousLine"

        if (throwOnRepeated) {
            throw new ValidationException(message)
        } else {
            log.warn message
        }
    }

    @Override
    @SuppressWarnings('CloseWithoutCloseable')
    void close() throws ItemStreamException {
        currentItemCount = 0
        seen = [:]
    }

    @Override
    void open(ExecutionContext executionContext) throws ItemStreamException {
        if (!saveState) {
            return
        }

        if (executionContext.containsKey(getExecutionContextKey(READ_COUNT))) {
            currentItemCount = executionContext.getInt(
                    getExecutionContextKey(READ_COUNT))
        }
        if (executionContext.containsKey(getExecutionContextKey(KEYS_MAP))) {
            seen = Maps.newHashMap(executionContext.get(
                    getExecutionContextKey(KEYS_MAP)))
        }
    }

    @Override
    void update(ExecutionContext executionContext) throws ItemStreamException {
        if (!saveState) {
            return
        }

        executionContext.putInt(getExecutionContextKey(READ_COUNT), currentItemCount)
        executionContext.put(getExecutionContextKey(KEYS_MAP), ImmutableMap.copyOf(seen))
    }
}
