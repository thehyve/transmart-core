package org.transmartproject.rest.serialization

import com.fasterxml.jackson.annotation.JsonCreator
import groovy.transform.CompileStatic

/**
 * Type to represent the requested serialization format.
 */
@CompileStatic
enum Format {
    JSON('application/json'),
    PROTOBUF('application/x-protobuf'),
    TSV('TSV'),
    SPSS('SPSS'),
    NONE('none')

    private String format

    Format(String format) {
        this.format = format
    }

    private static final Map<String, Format> mapping = new HashMap<>()
    static {
        for (Format format : values()) {
            mapping.put(format.format.toLowerCase(), format)
        }
    }

    @JsonCreator
    static Format from(String format) {
        format = format.toLowerCase()
        if (mapping.containsKey(format)) {
            return mapping[format]
        } else {
            return NONE
        }
    }

    String toString() {
        format
    }
}