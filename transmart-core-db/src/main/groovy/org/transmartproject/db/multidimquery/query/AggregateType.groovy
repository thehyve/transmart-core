/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.db.multidimquery.query

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Result types for observation queries.
 */
@CompileStatic
@Slf4j
enum AggregateType {
    MIN,
    MAX,
    AVERAGE,
    COUNT,
    NONE

    private static final Map<String, AggregateType> mapping = values().collectEntries {
        [(it.name().toLowerCase()): it]
    }

    public static AggregateType forName(String name) {
        name = name.toLowerCase()
        if (mapping.containsKey(name)) {
            return mapping[name]
        } else {
            // TODO(jan): Should this be an exception?
            log.error "Unknown aggregate type: ${name}"
            return NONE
        }
    }
}
