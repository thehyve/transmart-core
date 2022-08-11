package org.transmartproject.db.util

import com.google.common.collect.AbstractIterator
import com.google.common.collect.PeekingIterator

/**
 * Wrapper over an iterator that implements peeking iterator, or reuses original iterator if it implements peeking iterator already.
 */
class PeekingIteratorImpl/*<T>*/ extends AbstractIterator/*<T>*/ implements PeekingIterator {

    private final Iterator/*<T>*/ iterator

    private PeekingIteratorImpl(Iterator/*<T>*/ iterator) {
        this.iterator = iterator
    }

    static PeekingIterator getPeekingIterator(Iterator/*<T>*/ iterator) {
        if (iterator instanceof PeekingIterator) {
            iterator
        } else {
            new PeekingIteratorImpl/*<T>*/(iterator)
        }
    }

    @Override
    protected /*T*/ computeNext() {
        if (!iterator.hasNext()) {
            endOfData()
            return null
        }
        iterator.next()
    }
}
