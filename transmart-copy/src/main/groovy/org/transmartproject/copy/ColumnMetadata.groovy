package org.transmartproject.copy

import groovy.transform.CompileStatic
import groovy.transform.Immutable

/**
 * Metadata about a database column.
 * Contains the data type as Java class and a flag if the column is nullable.
 */
@Immutable
@CompileStatic
class ColumnMetadata {

    Class type

    Boolean nullable

}
