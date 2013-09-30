package org.transmartproject.db.ontology

import org.transmartproject.db.support.DatabasePortabilityService

import static org.transmartproject.db.support.DatabasePortabilityService.DatabaseType.ORACLE

/**
 * Properties that specify queries to be made in other tables. Used by
 * TableAccess and i2b2 metadata tables
 */
abstract class AbstractQuerySpecifyingType implements MetadataSelectQuerySpecification {

    String       factTableColumn
    String       dimensionTableName
    String       columnName
    String       columnDataType
    String       operator
    String       dimensionCode

    DatabasePortabilityService databasePortabilityService

    /**
     * Returns the SQL for the query that this object represents.
     *
     * @return raw SQL of the query that this type represents
     */
    String getQuerySql() {
        def res = "SELECT $factTableColumn " +
                "FROM $dimensionTableName " +
                "WHERE $columnName $operator $processedDimensionCode"
        if (operator.equalsIgnoreCase('like') &&
                databasePortabilityService.databaseType == ORACLE) {
            res += ' ESCAPE \'\\\''
        }
        res
    }

    /* implements (hopefully improved) transformations described here:
     * https://community.i2b2.org/wiki/display/DevForum/Query+Building+from+Ontology
     */
    String getProcessedDimensionCode() {
        def v = dimensionCode
        if (!v) {
            return v
        }

        if (columnDataType == 'T' && v.length() > 2) {
            if (operator.equalsIgnoreCase('like')) {
                if (v[0] != "'" && !v[0] != '(') {
                    if (v[-1] != '%') {
                        if (v[-1] != '\\') {
                            v += '\\'
                        }
                        v = v.asLikeLiteral() + '%'
                    }
                }
            }

            if (v[0] != "'") {
                v = v.replaceAll(/'/, "''") /* escape single quotes */
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
