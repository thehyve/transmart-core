package org.transmartproject.batch.db

import groovy.transform.Immutable
import groovy.transform.ToString

/**
 * Used to specify the column. @see DatabaseMetaDataService.getColumnDeclaration(spec)
 */
@Immutable
@ToString
class ColumnSpecification {

    String schema
    String table
    String column

}
