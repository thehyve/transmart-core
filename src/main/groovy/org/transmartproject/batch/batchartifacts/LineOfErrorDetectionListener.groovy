package org.transmartproject.batch.batchartifacts

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.springframework.batch.core.annotation.*
import org.springframework.dao.DataAccessException

/**
 * Listener that keeps track of the number of items read, processed and written,
 * so it can report those numbers in case of error.
 *
 * Doesn't handle skips at this point.
 *
 * TODO: implement ItemStream so we can keep counting across restarts
 * TODO: correctly handle retries (of chunks and more fine grained operations)
 */
@CompileStatic
@Slf4j
class LineOfErrorDetectionListener<T> {
    long read
    long processed
    long written

    @BeforeRead
    void beforeRead() {
        read++
    }

    @BeforeProcess
    @SuppressWarnings('UnusedMethodParameter')
    void beforeProcess(T item) {
        processed++
    }

    @OnReadError
    @SuppressWarnings('UnusedMethodParameter')
    void onReadError(Exception e) {
        log.warn("Read error occurred on item #$read")
    }

    @OnProcessError
    @SuppressWarnings('UnusedMethodParameter')
    void onProcessError(T item, Exception e) {
        log.warn("Processing error occurred after $read items have been " +
                "read and this was the item being processed #$processed")
    }

    @OnWriteError
    @CompileStatic(value = TypeCheckingMode.SKIP)
    void onWriteError(Exception e, List<? extends T> items) {
        log.warn("Write error occurred on after $read items have been " +
                "read, $processed processed; chunk of ${items.size()} was being written")

        /* erm... not really realated to the main function of this class, but
         * useful because postgresql insists on hiding the real error message */
        if (e instanceof DataAccessException && e?.cause?.respondsTo('getNextException')) {
            log.error("Next exception had message: ${e.cause.nextException.message}")
        }
    }

}
