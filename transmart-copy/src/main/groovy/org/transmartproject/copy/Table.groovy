package org.transmartproject.copy

import groovy.transform.CompileStatic
import groovy.transform.Immutable

import java.nio.file.FileSystems

/**
 * Captures the schema and name of a database table.
 */
@Immutable
@CompileStatic
class Table {
    String schema
    String name

    String getFileName() {
        FileSystems.default.getPath(schema, "${name}.tsv").toString()
    }

    @Override
    String toString() {
        schema ? "${schema}.${name}" : name
    }
}
