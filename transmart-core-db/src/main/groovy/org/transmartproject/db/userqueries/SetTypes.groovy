package org.transmartproject.db.userqueries

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Supported types of sets for user queries
 */
@CompileStatic
@Slf4j
enum SetTypes {
    PATIENT ('PATIENT')

    private String type

    SetTypes(String type) {
        this.type = type
    }

    static SetTypes from(String type) {
        SetTypes t = values().find { it.type == type }
        if (t == null) {
            log.warn "Unknown type of set: ${type}"
        }
        t
    }

    String value() {
        type
    }
}
