/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.util

import groovy.transform.CompileStatic
import org.hibernate.ScrollableResults
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.function.Function

@CompileStatic
class ScrollableResultsIterator<T> implements Iterator<T>, Closeable {

    private Logger logger = LoggerFactory.getLogger(getClass())
    private ScrollableResults scrollableResults
    private Function<Object,T> transform
    private Boolean hasNext = null
    private boolean closed

    ScrollableResultsIterator(ScrollableResults scrollableResults, Function<Object,T> transform=null) {
        this.scrollableResults = scrollableResults
        this.transform = transform
    }

    @Override
    boolean hasNext() {
        if (hasNext == null) {
            hasNext = scrollableResults.next()
            if(!hasNext) close()
        }
        hasNext
    }

    @Override
    T next() {
        if (hasNext()) {
            hasNext = null
            def elem = scrollableResults.get(0)
            if(transform != null) return transform.apply(elem)
            else return (T) elem
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
        if(!closed) this.scrollableResults.close()
        closed = true
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize()
        if (!closed) {
            logger.error('Failed to call close before the object was scheduled to be garbage collected')
            close()
        }
    }

}
