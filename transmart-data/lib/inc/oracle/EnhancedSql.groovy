/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-data.
 *
 * Transmart-data is free software: you can redistribute it and/or modify it
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
 * transmart-data.  If not, see <http://www.gnu.org/licenses/>.
 */

package inc.oracle

import groovy.sql.Sql

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.sql.Connection
import java.sql.SQLWarning
import java.sql.Statement

class EnhancedSql extends Sql {

    private String defaultSchema

    EnhancedSql(Sql sql) {
        super(sql)

        defaultSchema = fetchCurrentSchema()
    }

    String fetchCurrentSchema() {
        firstRow("SELECT sys_context('userenv', 'current_schema') FROM DUAL")[0]
    }

    void setCurrentSchema(String newSchema) {
        execute ("ALTER SESSION SET current_schema = $newSchema" as String)
    }

    void restoreCurrentSchema() {
        setCurrentSchema defaultSchema
    }

    Boolean executeAndDoWithStatement(String sqlStatement, Closure closure) {
        Connection conn = createConnection()
        Statement statement = null;
        try {
            Method method = Sql.getDeclaredMethod('getStatement', Connection, String)
            method.accessible = true
            statement = method.invoke(this, conn, sqlStatement)

            boolean isResultSet = statement.execute(sqlStatement)

            Field field = Sql.getDeclaredField('updateCount')
            field.accessible = true
            field.setInt this, statement.getUpdateCount()

            closure.call(statement)
            return isResultSet
        } finally {
            closeResources(conn, statement)
        }
    }

    Boolean executeAndPrintWarnings(String sqlStatement) {
        executeAndDoWithStatement(sqlStatement,
                EnhancedSql.&printWarnings.curry(sqlStatement))
    }

    static void printWarnings(String statementString, Statement statement) {
        SQLWarning warning = statement.getWarnings()
        def abbreviatedStatement = statementString[0..((Math.min(65, statementString.size() - 1)))]
        if (statementString.size() > 65) {
            abbreviatedStatement += '...'
        }
        if (warning) {
            Log.warn "Warnings for statement $abbreviatedStatement"
            while (warning) {
                Log.warn warning.message
                Log.warn "${warning.getErrorCode()}: ${warning.getSQLState()}"
                warning = warning.nextWarning
            }
        }
    }

}
