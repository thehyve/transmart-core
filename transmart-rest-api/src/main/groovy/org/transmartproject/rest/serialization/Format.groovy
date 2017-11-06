package org.transmartproject.rest.serialization

/**
 * Type to represent the requested serialization format.
 */
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

    static Format from(String format) {
        Format f = values().find { it.format == format }
        if (f == null) throw new Exception("Unknown format: ${format}")
        f
    }

    String toString() {
        format
    }
}