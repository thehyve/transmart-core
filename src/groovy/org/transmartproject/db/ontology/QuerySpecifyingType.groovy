package org.transmartproject.db.ontology

/**
 * Properties that specify queries to be made in other tables. Used by
 * TableAccess and i2b2 metada tables
 */
abstract class AbstractQuerySpecifyingType implements MetadataSelectQuerySpecification {

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

    /* implements transformations described here:
     * https://community.i2b2.org/wiki/display/DevForum/Query+Building+from+Ontology
     */
    String getProcessedDimensionCode() {
        def v = dimensionCode
        if (!v) {
            return v
        }

        if (columnDataType == 'T' && v.length() > 2) {
            if (operator.equalsIgnoreCase('like')) {
                if (v[0] != "'" && !v.contains('(')) {
                    if (v[-1] != '%') {
                        if (v[-1] != '\\') {
                            v += '\\'
                        }
                        v += '%'
                    }
                }
            }

            if (v[0] != "'") {
                v = "'$v'"
            }

        }

        if (operator.equalsIgnoreCase('in')) {
            v = "($v)"
        }

        v
    }

    static constraints = {
        factTableColumn      nullable:   false,   maxSize:   50
        dimensionTableName   nullable:   false,   maxSize:   50
        columnName           nullable:   false,   maxSize:   50
        columnDataType       nullable:   false,   maxSize:   50
        operator             nullable:   false,   maxSize:   10
        dimensionCode        nullable:   false,   maxSize:   700
    }
}
