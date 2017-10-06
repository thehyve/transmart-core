package org.transmartproject.db.util

import com.google.common.collect.AbstractIterator
import com.google.common.collect.PeekingIterator

/**
 * Groups rows that belong to the same group to one.
 */
abstract class AbstractGroupingIterator<I, T, K> extends AbstractIterator<I> implements PeekingIterator {

    private final PeekingIterator<T> iterator

    AbstractGroupingIterator(Iterator<T> iterator) {
        this.iterator = PeekingIteratorImpl.getPeekingIterator(iterator)
    }

    abstract K calculateGroupKey(T item)

    abstract I computeResultItem(K groupKey, Iterable<T> groupedItems)

    @Override
    protected I computeNext() {
        if (!iterator.hasNext()) {
            endOfData()
            return null
        }
        def groupedItems = new ArrayList<T>()
        groupedItems.add(iterator.next())
        def groupKey = calculateGroupKey(groupedItems.first())
        while (iterator.hasNext() && groupKey == calculateGroupKey(iterator.peek())) {
            groupedItems.add(iterator.peek())
            iterator.next()
        }
        computeResultItem(groupKey, groupedItems)
    }
}
