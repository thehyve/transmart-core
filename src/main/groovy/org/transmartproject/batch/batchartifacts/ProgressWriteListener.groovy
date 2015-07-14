package org.transmartproject.batch.batchartifacts

import groovy.util.logging.Slf4j
import org.springframework.batch.core.annotation.AfterWrite
import org.springframework.batch.core.annotation.BeforeWrite

/**
 * Logs progress messages.
 */
@Slf4j
class ProgressWriteListener<T> {
    private static final long MIN_DIFFERENCE = 1

    int everyNChunks = 1

    private long chunks = 0
    private long items = 0
    private long start
    private long last

    @BeforeWrite
    @SuppressWarnings('UnusedMethodParameter')
    void beforeWrite(List<? extends T> chunkItems) {
        if (start) {
            return
        }

        /* we should start counting before the first item is read,
          but any error will average out */
        start = last = System.currentTimeMillis()
    }

    @AfterWrite
    void afterWrite(List<? extends T> chunkItems) {
        if (!log.infoEnabled) {
            return
        }

        chunks++
        if (chunks % everyNChunks != 0) {
            return
        }

        def curTime = System.currentTimeMillis()

        def perSecondGlobal = (items + chunkItems.size()) /
                (Math.max(curTime - start, MIN_DIFFERENCE) / 1000)
        def perSecondLocal = (chunkItems.size()) /
                (Math.max(curTime - last, MIN_DIFFERENCE) / 1000)

        items += chunkItems.size()
        last = curTime

        log.info("Items written: $items, {} items/s (last); {} items/s (global)",
                String.format('%.2f', perSecondLocal),
                String.format('%.2f', perSecondGlobal))
    }
}
