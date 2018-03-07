package org.transmartproject.core.userquery

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Supported types of sets for user queries
 */
@CompileStatic
@Slf4j
enum SetType {
    PATIENT ('PATIENT')

    private String type

    SetType(String type) {
        this.type = type
    }

    static SetType from(String type) {
        SetType t = values().find { it.type == type }
        if (t == null) {
            log.warn "Unknown type of set: ${type}"
        }
        t
    }

    String value() {
        type
    }
}
