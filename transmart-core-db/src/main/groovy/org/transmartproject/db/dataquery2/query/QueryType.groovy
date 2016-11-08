package org.transmartproject.db.dataquery2.query

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Result types for observation queries.
 */
@CompileStatic
@Slf4j
enum QueryType {
    VALUES,
    MIN,
    MAX,
    AVERAGE,
    COUNT,
    EXISTS,
    NONE

    private static final Map<String, QueryType> mapping = values().collectEntries {
        [(it.name().toLowerCase()): it]
    }

    public static QueryType forName(String name) {
        name = name.toLowerCase()
        if (mapping.containsKey(name)) {
            return mapping[name]
        } else {
            // TODO(jan): Should this be an exception?
            log.error "Unknown query type: ${name}"
            return NONE
        }
    }
}
