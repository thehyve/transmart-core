package org.transmartproject.db.dataquery.highdim

import com.google.common.collect.AbstractIterator
import com.google.common.collect.Iterators
import com.google.common.collect.PeekingIterator
import org.transmartproject.core.dataquery.DataColumn
import org.transmartproject.core.dataquery.TabularResult

class RepeatedEntriesCollectingTabularResult<T extends AbstractDataRow> {

    @Delegate
    TabularResult<DataColumn, T> tabularResult

    Closure<Object> collectBy = Closure.IDENTITY

    Closure<T> resultItem = { it[0] }

    Iterator<T> getRows() {
        new RepeatedEntriesCollectingIterator(tabularResult.iterator())
    }

    Iterator<T> iterator() {
        getRows()
    }

    public class RepeatedEntriesCollectingIterator extends AbstractIterator<T> {

        PeekingIterator<T> sourceIterator

        RepeatedEntriesCollectingIterator(Iterator<T> sourceIterator) {
            this.sourceIterator = Iterators.peekingIterator sourceIterator
        }

        @Override
        protected T computeNext() {
            List<T> collected = []
            if (!sourceIterator.hasNext()) {
                endOfData()
                return
            }

            collected << sourceIterator.next()
            while (sourceIterator.hasNext() &&
                    collectBy(sourceIterator.peek()) != null &&
                    collectBy(sourceIterator.peek()) == collectBy(collected[0])) {
                collected << sourceIterator.next()
            }

            resultItem(collected)
        }
    }
}
