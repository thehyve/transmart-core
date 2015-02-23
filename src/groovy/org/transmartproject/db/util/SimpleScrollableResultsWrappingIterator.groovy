package org.transmartproject.db.util

import groovy.util.logging.Log4j
import org.hibernate.ScrollableResults

@Log4j
class SimpleScrollableResultsWrappingIterator<T> implements Closeable, Iterable<T> {

    private final SimpleScrollableResultsIterator scrollableResultsIterator
    private boolean closed
    private boolean iteratorIsCalled

    SimpleScrollableResultsWrappingIterator(ScrollableResults scrollableResults) {
        this.scrollableResultsIterator = new SimpleScrollableResultsIterator()
        this.scrollableResultsIterator.scrollableResults = scrollableResults
    }

    @Override
    Iterator<T> iterator() {
        if (iteratorIsCalled) {
            throw new IllegalStateException('Cannot be called more than once.')
        }
        iteratorIsCalled = true
        scrollableResultsIterator
    }

    @Override
    void close() throws IOException {
        this.scrollableResultsIterator.scrollableResults.close()
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

    class SimpleScrollableResultsIterator implements Iterator<T> {

        private ScrollableResults scrollableResults
        private Boolean hasNext = null

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
                scrollableResults.get(0)
            } else {
                throw new NoSuchElementException()
            }
        }

        @Override
        void remove() {
            throw new UnsupportedOperationException()
        }

    }

}
