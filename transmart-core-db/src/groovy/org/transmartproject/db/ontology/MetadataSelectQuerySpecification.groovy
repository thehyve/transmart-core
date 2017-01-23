/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.ontology

import org.transmartproject.db.user.User

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
     * The type of data returned by {@link #getDimensionCode()}}/
     *
     * @return
     */
    String getColumnDataType()

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

    /**
     * Do any necessary post processing to the query based on the fact
     * that the issuing user is the one passed in.
     *
     * @param original the original query, constructed based on the
     * properties of this object
     * @param user the user issuing the query
     * @return the original or a modified SQL statement
     */
    String postProcessQuery(String original, User user)
}
