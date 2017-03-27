/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.util

import groovy.util.logging.Slf4j

@Slf4j
abstract class AbstractOneTimeCallIterable<T> implements Iterable<T> {

    private boolean iteratorIsCalled

    protected abstract Iterator getIterator()

    @Override
    Iterator<T> iterator() {
        if (iteratorIsCalled) {
            throw new IllegalStateException('Cannot be called more than once.')
        }
        iteratorIsCalled = true
        iterator
    }

}
