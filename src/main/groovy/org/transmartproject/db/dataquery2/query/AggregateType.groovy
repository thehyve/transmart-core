package org.transmartproject.db.dataquery2.query

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

    private static final Map<String, AggregateType> mapping = new HashMap<>();
    static {
        for (AggregateType type: values()) {
            mapping.put(type.name().toLowerCase(), type);
        }
    }

    public static AggregateType forName(String name) {
        name = name.toLowerCase()
        if (mapping.containsKey(name)) {
            return mapping[name]
        } else {
            log.error "Unknown aggregate type: ${name}"
            return NONE
        }
    }
}
