/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.core.multidimquery

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Result types for observation queries.
 */
@Slf4j
@CompileStatic
enum AggregateType {
    MIN,
    MAX,
    AVERAGE,
    COUNT,
    PATIENT_COUNT,
    STD_DEV,

    private static final Map<String, AggregateType> mapping = values().collectEntries {
        [(it.name().toLowerCase()): it]
    }

    String toString() {
        name().toLowerCase()
    }

    static AggregateType forName(String name) {
        String key = name.toLowerCase()
        if (!mapping.containsKey(key)) {
            throw new IllegalArgumentException("Unknown aggregate type: $name")
        }
        return mapping[key]
    }
}
