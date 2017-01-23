package org.transmartproject.db.util

import groovy.transform.CompileStatic
import groovy.util.logging.Log4j
import org.hibernate.ScrollableResults

@CompileStatic
@Log4j
class ScrollableResultsIterator<T> implements Iterator<T>, Closeable {

    private ScrollableResults scrollableResults
    private Boolean hasNext = null
    private boolean closed

    ScrollableResultsIterator(ScrollableResults scrollableResults) {
        this.scrollableResults = scrollableResults
    }

    @Override
    boolean hasNext() {
        if (hasNext == null) {
            hasNext = scrollableResults.next()
        } else {
            hasNext
        }
    }

    @Override
    T next() {
        if (hasNext()) {
            hasNext = null
            (T) scrollableResults.get(0)
        } else {
            throw new NoSuchElementException()
        }
    }

    @Override
    void remove() {
        throw new UnsupportedOperationException()
    }

    @Override
    void close() throws IOException {
        this.scrollableResults.close()
        closed = true
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize()
        if (!closed) {
            log.error('Failed to call close before the object was scheduled to ' +
                    'be garbage collected')
            close()
        }
    }

}
