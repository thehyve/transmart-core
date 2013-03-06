package org.transmartproject.db.ontology

/**
 * Properties that specify queries to be made in other tables. Used by
 * TableAccess and i2b2 metada tables
 */
abstract class AbstractQuerySpecifyingType {

    String       factTableColumn
    String       dimensionTableName
    String       columnName
    String       columnDataType
    String       operator
    String       dimensionCode

    /*
     * SELECT [factTableColumn]
     *   FROM [dimensionTableName]
     *   WHERE [columnName] [operator] [dimensionCode]
     */

    static constraints = {
        factTableColumn      nullable:   false,   maxSize:   50
        dimensionTableName   nullable:   false,   maxSize:   50
        columnName           nullable:   false,   maxSize:   50
        columnDataType       nullable:   false,   maxSize:   50
        operator             nullable:   false,   maxSize:   10
        dimensionCode        nullable:   false,   maxSize:   700
    }
}
