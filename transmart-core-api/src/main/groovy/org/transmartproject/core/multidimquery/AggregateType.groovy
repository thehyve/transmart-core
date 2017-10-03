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
    VALUES

    private static final Map<String, AggregateType> mapping = values().collectEntries {
        [(it.name().toLowerCase()): it]
    }

    public String toString() {
        name().toLowerCase()
    }

    public static AggregateType forName(String name) {
        name = name.toLowerCase()
        return mapping[name] ?: {
            throw new IllegalArgumentException("Unknown aggregate type: $name")
            null as AggregateType // because Groovy wants a return type
        }()
    }
}
