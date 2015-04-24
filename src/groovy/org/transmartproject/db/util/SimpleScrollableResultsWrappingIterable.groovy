package org.transmartproject.db.util

import org.hibernate.ScrollableResults

class SimpleScrollableResultsWrappingIterable<T> extends AbstractOneTimeCallIterable<T> implements Closeable {

    protected final SimpleScrollableResultsIterator scrollableResultsIterator

    SimpleScrollableResultsWrappingIterable(ScrollableResults scrollableResults) {
        this.scrollableResultsIterator = new SimpleScrollableResultsIterator(scrollableResults)
    }

    @Override
    protected Iterator getIterator() {
        scrollableResultsIterator
    }

    @Override
    void close() throws IOException {
        scrollableResultsIterator.close()
    }

}
