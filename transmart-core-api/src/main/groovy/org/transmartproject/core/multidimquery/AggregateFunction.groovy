/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Aggregate functions to run on numerical observations.
 */
@Slf4j
@CompileStatic
enum AggregateFunction {
    MIN,
    MAX,
    AVERAGE,
    COUNT,
    STD_DEV,

    private static final Map<String, AggregateFunction> mapping = values().collectEntries {
        [(it.name().toLowerCase()): it]
    }

    String toString() {
        name().toLowerCase()
    }

    static AggregateFunction forName(String name) {
        String key = name.toLowerCase()
        if (!mapping.containsKey(key)) {
            throw new IllegalArgumentException("Unknown aggregate type: $name")
        }
        return mapping[key]
    }
}
