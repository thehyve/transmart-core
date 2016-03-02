package org.transmartproject.db.util

import org.hibernate.ScrollableResults
import org.transmartproject.core.IterableResult

class SimpleScrollableResultsWrappingIterable<T> extends AbstractOneTimeCallIterable<T> implements IterableResult<T> {

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
