package org.transmartproject.db.ontology

/**
 * Specifies an i2b2 metadata SQL query in the form:
 *
 * <code>
 *     SELECT [factTableColumn]
 *       FROM [dimensionTableName]
 *       WHERE [columnName] [operator] [dimensionCode]
 * </code>
 */
interface MetadataSelectQuerySpecification {

    /**
     * The column that exists both in the dimension table and in the fact table.
     * The values obtained for this column in the dimension table under the
     * query specified by this object will be used to filter the fact table.
     *
     * @return the name of the fact table column, which exists also in the
     * dimension table and which will be projection of this query
     */
    String getFactTableColumn()

    /**
     * The name of the dimension table; the table that will be queried.
     *
     * @return the dimension table name
     */
    String getDimensionTableName()

    /**
     * The column that be used to filter the results of this query.
     *
     * @return the WHERE clause column
     */
    String getColumnName()

    /**
     * The operator that will be used to filter the results; the operand will
     * be the column name and the dimension code
     *
     * @return the operator used in the WHERE clause
     */
    String getOperator()

    /**
     * The SQL fragment that will be used to filter the results,
     * together with the column name and the operator.
     *
     * @return the second operand of the WHERE clause
     */
    String getDimensionCode()
}
