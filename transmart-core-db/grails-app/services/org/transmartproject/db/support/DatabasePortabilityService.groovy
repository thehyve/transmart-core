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

package org.transmartproject.db.support

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.Criteria
import org.hibernate.criterion.LikeExpression
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import javax.annotation.PostConstruct
import javax.sql.DataSource
import java.sql.Connection
import java.sql.DatabaseMetaData

/**
 * Helper service to make it easier to write code that works on both Oracle and
 * PostgreSQL. Of course, the best option in this respect is to use Hibernate.
 */
class DatabasePortabilityService {

    @Autowired
    @Qualifier("dataSource")
    DataSource dataSource

    DatabaseType databaseType;

    enum DatabaseType {
        POSTGRESQL,
        ORACLE
    }

    private runCorrectImplementation(Closure postgresImpl, Closure oracleImpl) {
        switch (getDatabaseType()) {
            case DatabaseType.POSTGRESQL:
                return postgresImpl()
            case DatabaseType.ORACLE:
                return oracleImpl()
            default:
                throw new IllegalStateException("Should not reach this point. " +
                        "Value of databaseType is $databaseType")
        }
    }

    /**
     * The operator that computes the relative complement of two sets, like
     * MINUS in Oracle and EXCEPT in PostgreSQL.
     *
     * @return the relative complement operator
     */
    String getComplementOperator() {
        runCorrectImplementation(
                { 'EXCEPT' },
                { 'MINUS' }
        )
    }

    String getCurrentDateTimeFunc() {
        runCorrectImplementation(
                { 'now()' },
                { 'sysdate' }
        )
    }

    String createTopNQuery(String s) {
        runCorrectImplementation(
                { "$s LIMIT ?" },
                { "SELECT * FROM ($s) WHERE ROWNUM <= ?" }
        )
    }

    /**
     * Create pagination query.
     * It is important that the query is doing a sort by something unique!
     * That is, there should not be rows comparing equal.
     *
     * @param s the string to transform
     * @param rowNumberColName the name of the column with the row index,
     *                         or null for none
     * @return the transformed query
     */
    String createPaginationQuery(String s, String rowNumberColName=null) {
        runCorrectImplementation(
                /* PostgreSQL */
                {
                    if (rowNumberColName == null) {
                        "$s LIMIT ? OFFSET ?"
                    } else {
                        """
                        SELECT
                            row_number() OVER () AS $rowNumberColName, *
                        FROM ( $s ) pag_a
                        LIMIT ?
                        OFFSET ?
                        """
                    }
                },
                /* Oracle */
                {
                    String rowColumnFragment = ""
                    if (rowNumberColName != null) {
                        rowColumnFragment = ", rnum AS $rowNumberColName"
                    }

                    /* see http://www.oracle.com/technetwork/issue-archive/2006/06-sep/o56asktom-086197.html */
                    """
                    SELECT
                        *$rowColumnFragment
                    FROM (
                            SELECT
                                /*+ FIRST_ROWS(n) */
                                pag_a.*,
                                ROWNUM rnum
                            FROM ( $s ) pag_a
                            WHERE
                                ROWNUM <= ? /* last row to include */ )
                    WHERE
                        rnum >= ? /* first row to include */"""
                }
        )
    }

    /* Convert limit into Oracle's first row number to exclude, if applicable */
    List convertLimitStyle(Number limit, Number offset) {
        runCorrectImplementation(
                { [limit, offset] }, /* do not convert for PostgreSQL */
                { [offset + limit, offset + 1] }
        )
    }

    String toChar(String expr){
        runCorrectImplementation(
                {"CAST($expr as character varying)"},
                {"to_char($expr)"}
        )
    }

    /**
     * Convert pagination limits for use with queries transformed with the
     * methods available in this class.
     *
     * @param start starting index, 1-based, inclusive
     * @param end ending index, 1-based, inclusive
     * @return list with two elements in the order they should be in order to
     * replace the placeholders in the queries generated by this class methods
     */
    List convertRangeStyle(Number start /* incl, 1-based */, Number end /* incl */) {
        runCorrectImplementation(
                { [end - start + 1, start - 1] },
                { [end, start] } /* do not convert for Oracle */
        )
    }

    /**
     * The SQL fragment for obtaining the next value in a sequence.
     *
     * @return sql fragment
     */
    String getNextSequenceValueSql(String schema, String sequenceName) {
        runCorrectImplementation(
                { "SELECT nextval('${schema}.${sequenceName}')" },
                { "SELECT ${schema}.${sequenceName}.nextval FROM DUAL" }
        )
    }

    @PostConstruct
    void init() {
        Connection connection = dataSource.getConnection()
        DatabaseMetaData metaData
        try {
            metaData = connection.metaData
        } finally {
            connection.close()
        }
        def databaseName = metaData.databaseProductName.toLowerCase()

        switch (databaseName) {
        case ~/postgresql.*/:
            databaseType = DatabaseType.POSTGRESQL
            break

        case ~/oracle.*/:
            databaseType = DatabaseType.ORACLE
            break

        default:
            log.warn 'Could not detect data source driver as either ' +
                    'PostgreSQL or Oracle; defaulting to PostgreSQL ' +
                    '(this is OK if running H2 in Postgres compatibility ' +
                    'mode)'
            databaseType = DatabaseType.POSTGRESQL
        }

        log.debug 'Selected database type is ' + databaseType

        doFixups()
    }

    private void doFixups() {
        if (databaseType == DatabaseType.ORACLE) {
            fixupLikeOracle()
        }
    }

    private void fixupLikeOracle() {
        /* Oracle does not use an escape character for LIKE by default. The escape
         * character has to be explicitly set in the SQL statement. Fixup the like
         * criterion so that such escape character is included. For consistency with
         * PostgreSQL, use the backslash as the escape character */

        Criteria.metaClass.like = HibernateCriteriaBuilder.metaClass.like = {
            String propertyName, String propertyValue ->
            /* beware, if the second arg is not a string, then this implementation is not used! */

            if (!delegate.validateSimpleExpression()) {
                throwRuntimeException(new IllegalArgumentException("Call to [like] with propertyName [" +
                        propertyName + "] and value [" + propertyValue + "] not allowed here."));
            }

            propertyName = delegate.calculatePropertyName propertyName
            propertyValue = delegate.calculatePropertyValue propertyValue
            delegate.addToCriteria(new LikeExpression(propertyName, propertyValue, '\\', false) {})
            delegate
        }
    }
}
