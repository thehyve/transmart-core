package org.transmartproject.db.util

import com.google.common.collect.AbstractIterator
import com.google.common.collect.PeekingIterator

/**
 * Groups rows that belong to the same group to one.
 */
abstract class AbstractGroupingIterator<K, T> extends AbstractIterator<K> implements PeekingIterator {

    private final PeekingIterator<T> iterator

    AbstractGroupingIterator(Iterator<T> iterator) {
        this.iterator = PeekingIteratorImpl.getPeekingIterator(iterator)
    }

    abstract boolean isTheSameGroup(T item1, T item2)

    abstract K computeResultItem(Iterable<T> groupedItems)

    @Override
    protected K computeNext() {
        if (!iterator.hasNext()) {
            endOfData()
            return null
        }
        def groupedItems = new ArrayList<T>()
        groupedItems.add(iterator.next())
        while (iterator.hasNext() && isTheSameGroup(groupedItems.first(), iterator.peek())) {
            groupedItems.add(iterator.peek())
            iterator.next()
        }
        computeResultItem(groupedItems)
    }
}
