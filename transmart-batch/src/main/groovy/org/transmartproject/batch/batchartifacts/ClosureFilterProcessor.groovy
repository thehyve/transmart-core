package org.transmartproject.batch.batchartifacts

import org.springframework.batch.item.ItemProcessor

/**
 * Filter items based on logic provided through a closure.
 * The closure
 */
class ClosureFilterProcessor<T> implements ItemProcessor<T, T> {

    private final Closure<Boolean> whetherToIncludeClosure

    ClosureFilterProcessor(Closure<Boolean> whetherToIncludeClosure) {
        this.whetherToIncludeClosure = whetherToIncludeClosure
    }

    @Override
    T process(T item) throws Exception {
        if (whetherToIncludeClosure.call(item)) {
            item
        } // else null
    }
}
