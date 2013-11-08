package inc

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
            Log.err "Warnings for statement $abbreviatedStatement"
            while (warning) {
                Log.err warning.message
                warning = warning.nextWarning
            }
        }
    }

}
