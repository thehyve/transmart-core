package inc

import groovy.sql.Sql

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLWarning
import java.sql.Statement

import static java.lang.System.getenv
import static groovyx.gpars.GParsPool.withPool

class SqlProducer {
    static Sql createFromEnv(String user = null, String password = null, Boolean enhance = false) {
        Class.forName 'oracle.jdbc.driver.OracleDriver'
        def url = "jdbc:oracle:thin:@${getenv 'ORAHOST'}:${getenv 'ORAPORT'}:${getenv "ORASID"}"

        Connection connection = DriverManager.getConnection(url,
                user ?: getenv('ORAUSER'),
                password ?: getenv('ORAPASSWORD'))
        connection.autoCommit = false
        /* by creating an Sql from a Connection, we ensure only that connection
         * is used by the Sql; it will not generate connections by itself.
         * The effect is basically the same as if we were running always under
         * Sql::cacheConnection() */
        def sql = new Sql(connection)
        if (enhance) {
            String defaultSchema = fetchCurrentSchema sql
            sql.metaClass.setCurrentSchema = { newSchema ->
                delegate.execute ("ALTER SESSION SET current_schema = $newSchema" as String)
            }
            sql.metaClass.restoreCurrentSchema = { ->
                delegate.setCurrentSchema defaultSchema
            }

            sql.metaClass.executeAndDoWithStatement = { String sqlStatement, Closure closure ->
                Connection conn = delegate.createConnection()
                Statement statement = null;
                try {
                    statement = delegate.getStatement(conn, sqlStatement)
                    boolean isResultSet = statement.execute(sqlStatement)
                    delegate.updateCount = statement.getUpdateCount()
                    closure.call(statement)
                    return isResultSet
                } finally {
                    delegate.closeResources(conn, statement)
                }
            }

            sql.metaClass.executeAndPrintWarnings = { String sqlStatement ->
                delegate.executeAndDoWithStatement(sqlStatement,
                        SqlProducer.&printWarnings.curry(sqlStatement))
            }
        }
        sql
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

    static List<Sql> createMultipleFromEnv(Integer n,
                                           String user = null,
                                           String password = null,
                                           Boolean enhance = false) {
        withPool n, {
            def sqlProducerAsync = SqlProducer.&createFromEnv.curry(user, password, enhance).asyncFun()
            Collections.nCopies(n, sqlProducerAsync)*.call()*.get()
        }
    }

    static String fetchCurrentSchema(Sql sql) {
        sql.firstRow("SELECT sys_context('userenv', 'current_schema') FROM DUAL")[0]
    }

    static closeConnections(Collection<Sql> sqls, Closure beforeShutdown = null) {
        if (sqls.empty) {
            return
        }

        withPool sqls.size(), {
            sqls.eachParallel { Sql sql ->
                if (beforeShutdown) {
                    beforeShutdown.call sql
                }
                sql.close()
            }
        }
    }
}
