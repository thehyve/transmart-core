package org.transmartproject.copy

import groovy.transform.Immutable

import java.nio.file.FileSystems

@Immutable
class Table {
    String schema
    String name

    String getFileName() {
        FileSystems.default.getPath(schema, "${name}.tsv").toString()
    }

    @Override
    String toString() {
        "${schema}.${name}"
    }
}
