/*
 * Copyright (c) 2017  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy

import groovy.sql.Sql
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection
import org.transmartproject.copy.exception.InvalidState

import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant

import static java.lang.System.getenv

@Slf4j
@CompileStatic
class Database {

    static final Class getClassForType(String dataTypeName) {
        if (dataTypeName == null) {
            null
        } else {
            switch(dataTypeName.trim().toLowerCase()) {
                case 'numeric':
                    return Double.class
                case 'integer':
                case 'int8':
                case 'int4':
                    return Integer.class
                case 'bigint':
                case 'serial':
                    return Long.class
                case 'varchar':
                case 'text':
                case 'char':
                case 'bpchar':
                    return String.class
                case 'timestamp':
                    return Instant.class
                default:
                    throw new InvalidState("Unknown data type name: ${dataTypeName}.")
            }
        }
    }

    Connection connection
    CopyManager copyManager
    Sql sql

    void init() {
        String host = getenv('PGHOST')
        if (!host || host == '/tmp') {
            host = 'localhost'
        }
        String port = getenv('PGPORT') ?: '5432'
        String database = getenv('PGDATABASE') ?: 'transmart'
        String url = "jdbc:postgresql://${host}:${port}/${database}"
        String tm_cz_password = getenv('TM_CZ_PWD') ?: 'tm_cz'
        this.connection = DriverManager.getConnection(url, 'tm_cz', tm_cz_password)

        log.info "Connected to database ${url}."

        this.sql = new Sql(this.connection)
        this.copyManager = new CopyManager(this.connection as BaseConnection)
    }

    boolean tableExists(String table) {
        log.debug "Check if table ${table} exists ..."
        List<String> parts = table.tokenize('.')
        assert (parts.size() == 2)
        def schemaDef = parts[0]
        def tableDef = parts[1]
        def resultSet = connection.metaData.getTables(null, schemaDef, tableDef)
        resultSet.next()
    }

    LinkedHashMap<String, Class> getColumnMetadata(String table) {
        log.debug "Fetching metadata for ${table} ..."
        List<String> parts = table.tokenize('.')
        assert (parts.size() == 2)
        def schemaDef = parts[0]
        def tableDef = parts[1]
        def resultSet = connection.metaData.getColumns(null, schemaDef, tableDef, null)
        def result = [:] as LinkedHashMap
        while (resultSet.next()) {
            String columnName   = resultSet.getString('COLUMN_NAME')
            String dataTypeName = resultSet.getString('TYPE_NAME')
            result[columnName] = getClassForType(dataTypeName)
        }
        result
    }

    void copyFile(String tableName, File file, int rowCount) {
        file.withReader { reader ->
            def progressReportingReader = new ProgressReportingReader(reader, tableName, rowCount)
            log.info "Loading into ${tableName} from ${file} ..."
            long count = copyManager.copyIn("COPY ${tableName} FROM STDIN CSV DELIMITER E'\t'", progressReportingReader)
            progressReportingReader.progressBar.stop()
            log.info "${count} rows inserted."
        }
    }

    Long insertEntry(String table, LinkedHashMap<String, Class> columns, String idColumn, Map<String, Object> data) {
        def insertColumns = columns.collect { it.key }.findAll { it != idColumn }
        def statement = "insert into ${table} " +
                "(${insertColumns.join(', ')}) " +
                "values (${insertColumns.collect { ":${it}" }.join(', ')})".toString()

        def result = sql.executeInsert(data, statement, [idColumn])
        assert result.size() == 1
        result[0][idColumn] as Long
    }

    void insertEntry(String table, LinkedHashMap<String, Class> columns, Map<String, Object> data) {
        def insertColumns = columns.collect { it.key }
        def statement = "insert into ${table} " +
                "(${insertColumns.join(', ')}) " +
                "values (${insertColumns.collect { ":${it}" }.join(', ')})".toString()

        def result = sql.executeInsert(data, statement)
        assert result.size() == 1
    }

    void vacuumAnalyze() {
        log.info 'Running VACUUM ANALYZE ...'
        sql.execute('vacuum analyze')
        log.info 'Done.'
    }

}
