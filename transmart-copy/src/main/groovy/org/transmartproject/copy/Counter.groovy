package org.transmartproject.copy

import groovy.transform.CompileStatic

/**
 * Counter
 */
@CompileStatic
class Counter {
    private int value = 0

    int getValue() {
        value
    }

    void increment() {
        value++
    }

}
