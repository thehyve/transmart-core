/*
 * Copyright (c) 2017  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.postgresql.copy.CopyManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.transmartproject.copy.exception.InvalidState

import java.sql.Connection
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

    static final int batchSize = 500
    static final DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition()

    final HikariConfig config = new HikariConfig()
    final HikariDataSource dataSource
    final PlatformTransactionManager transactionManager
    final Connection connection
    final JdbcTemplate jdbcTemplate
    final NamedParameterJdbcTemplate namedParameterJdbcTemplate
    final CopyManager copyManager

    Database() {
        String host = getenv('PGHOST')
        if (!host || host == '/tmp') {
            host = 'localhost'
        }
        String port = getenv('PGPORT') ?: '5432'
        String database = getenv('PGDATABASE') ?: 'transmart'
        String url = "jdbc:postgresql://${host}:${port}/${database}"
        String tm_cz_password = getenv('TM_CZ_PWD') ?: 'tm_cz'

        config.jdbcUrl = url
        config.username = 'tm_cz'
        config.password = tm_cz_password
        config.autoCommit = false
        dataSource = new HikariDataSource(config)

        def dataSourceTransactionManager = new DataSourceTransactionManager()
        dataSourceTransactionManager.setDataSource(dataSource)
        transactionManager = dataSourceTransactionManager

        this.connection = DataSourceUtils.getConnection(dataSource)
        this.jdbcTemplate = new JdbcTemplate(dataSource)
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource)
        log.info "Connected to database ${url}."
    }

    TransactionStatus beginTransaction() {
        transactionManager.getTransaction(transactionDefinition)
    }

    void commit(TransactionStatus tx) {
        transactionManager.commit(tx)
    }

    boolean tableExists(Table table) {
        log.debug "Check if table ${table} exists ..."
        def resultSet = connection.metaData.getTables(null, table.schema, table.name)
        resultSet.next()
    }

    LinkedHashMap<String, Class> getColumnMetadata(Table table) {
        log.debug "Fetching metadata for ${table} ..."
        def resultSet = connection.metaData.getColumns(null, table.schema, table.name, null)
        def result = [:] as LinkedHashMap
        while (resultSet.next()) {
            String columnName   = resultSet.getString('COLUMN_NAME')
            String dataTypeName = resultSet.getString('TYPE_NAME')
            result[columnName] = getClassForType(dataTypeName)
        }
        result
    }

    static Map<String, Object> getValueMap(final LinkedHashMap<String, Class> columns, final String[] row) {
        assert columns.size() == row.length
        def result = [:] as Map<String, Object>
        int i = 0
        for(Map.Entry<String, Class> column: columns) {
            String columnName = column.key
            Class columnType = column.value
            if (row[i] == null || (row[i].empty && columnType != String.class)) {
                result[columnName] = null
            } else {
                def value
                switch(columnType) {
                    case String.class:
                        value = row[i]
                        break
                    case Instant.class:
                        value = Util.parseDate(row[i])
                        break
                    case Double.class:
                        value = Double.parseDouble(row[i])
                        break
                    case Integer.class:
                        value = Integer.parseInt(row[i])
                        break
                    case Long.class:
                        value = Long.parseLong(row[i])
                        break
                    default:
                        throw new InvalidState("Unexpected type ${columnType}")
                }
                result[columnName] = value
            }
            i++
        }
        result
    }

    final Map<Table, SimpleJdbcInsert> idGeneratingInserters = [:]

    SimpleJdbcInsert getIdGeneratingInserter(Table table, LinkedHashMap<String, Class> columns, String idColumn) {
        SimpleJdbcInsert inserter = idGeneratingInserters[table]
        if (!inserter) {
            log.debug "Creating id generating inserter for ${table} ..."
            inserter = new SimpleJdbcInsert(dataSource)
                .withSchemaName(table.schema)
                .withTableName(table.name)
                .usingColumns(columns.collect { it.key }.findAll { it != idColumn }.toArray() as String[])
                .usingGeneratedKeyColumns(idColumn)
            inserters[table] = inserter
        }
        inserter
    }

    Long insertEntry(Table table, LinkedHashMap<String, Class> columns, String idColumn, Map<String, Object> data) {
        def inserter = getIdGeneratingInserter(table, columns, idColumn)
        inserter.executeAndReturnKey(data) as Long
    }

    final Map<Table, SimpleJdbcInsert> inserters = [:]

    SimpleJdbcInsert getInserter(Table table, LinkedHashMap<String, Class> columns) {
        SimpleJdbcInsert inserter = inserters[table]
        if (!inserter) {
            log.debug "Creating inserter for ${table} ..."
            inserter = new SimpleJdbcInsert(dataSource)
                    .withSchemaName(table.schema)
                    .withTableName(table.name)
                    .usingColumns(columns.collect { it.key }.toArray() as String[])
            inserters[table] = inserter
        }
        inserter
    }

    void insertEntry(Table table, LinkedHashMap<String, Class> columns, Map<String, Object> data) {
        getInserter(table, columns).execute(data)
    }

    void executeCommand(String command) {
        log.debug "Executing command: ${command} ..."
        def conn = DataSourceUtils.getConnection(dataSource)
        conn.autoCommit = true
        conn.createStatement().execute('vacuum analyze')
        conn.autoCommit = false
    }

    void vacuumAnalyze() {
        log.info 'Running VACUUM ANALYZE ...'
        executeCommand('vacuum analyze')
        log.info 'Done.'
    }

}
