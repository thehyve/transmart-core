package org.transmartproject.db.util

import org.hibernate.ScrollableResults
import org.transmartproject.core.IterableResult

class ScrollableResultsWrappingIterable<T> extends AbstractOneTimeCallIterable<T> implements IterableResult<T> {

    protected final ScrollableResultsIterator scrollableResultsIterator

    ScrollableResultsWrappingIterable(ScrollableResults scrollableResults) {
        this.scrollableResultsIterator = new ScrollableResultsIterator(scrollableResults)
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
