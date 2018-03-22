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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.DefaultTransactionDefinition

import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant

@Slf4j
@CompileStatic
class Database implements AutoCloseable {

    static final Class getClassForType(String dataTypeName) {
        if (dataTypeName == null) {
            null
        } else {
            switch(dataTypeName.trim().toLowerCase()) {
                case 'bool':
                    return Boolean.class
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
                    throw new IllegalArgumentException("Unknown data type name: ${dataTypeName}.")
            }
        }
    }

    static final int defaultBatchSize = 500
    static final int defaultFlushSize = 1000
    static final DefaultTransactionDefinition transactionDefinition =
            new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED)

    final HikariConfig config = new HikariConfig()
    final HikariDataSource dataSource
    final PlatformTransactionManager transactionManager
    final Connection connection
    final JdbcTemplate jdbcTemplate
    final NamedParameterJdbcTemplate namedParameterJdbcTemplate

    Database(Map<String, String> params) {
        String host = params['PGHOST']
        if (!host || host == '/tmp') {
            host = 'localhost'
        }
        String port = params['PGPORT'] ?: '5432'
        String database = params['PGDATABASE'] ?: 'transmart'
        String url = "jdbc:postgresql://${host}:${port}/${database}"
        String username
        String password
        username = params['PGUSER']
        password = params['PGPASSWORD']
        if (!username) {
            throw new IllegalArgumentException('Please set the PGUSER environment variable.')
        }
        if (!password) {
            throw new IllegalArgumentException('Please set the PGPASSWORD environment variable.')
        }

        config.jdbcUrl = url
        config.username = username
        config.password = password
        config.autoCommit = false
        dataSource = new HikariDataSource(config)

        def dataSourceTransactionManager = new DataSourceTransactionManager()
        dataSourceTransactionManager.setDataSource(dataSource)
        transactionManager = dataSourceTransactionManager

        this.connection = DataSourceUtils.getConnection(dataSource)
        this.jdbcTemplate = new JdbcTemplate(dataSource)
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource)
        log.info "Connected to database ${url} as ${username} user."
    }

    TransactionStatus beginTransaction() {
        transactionManager.getTransaction(transactionDefinition)
    }

    void commit(TransactionStatus tx) {
        transactionManager.commit(tx)
    }

    void rollback(TransactionStatus tx) {
        transactionManager.rollback(tx)
    }

    boolean tableExists(Table table) {
        log.debug "Check if table ${table} exists ..."
        def resultSet = connection.metaData.getTables(null, table.schema, table.name, null)
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

    SimpleJdbcInsert getInserter(Table table, Map<String, Class> columns) {
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
        conn.createStatement().execute(command)
        conn.autoCommit = false
    }

    void vacuumAnalyze() {
        log.info 'Running VACUUM ANALYZE ...'
        executeCommand('vacuum analyze')
        log.info 'Done.'
    }

    Set<Table> getChildTables(Table parentTable) {
        log.debug "Getting child tables for: ${parentTable} ..."
        def queryResult = jdbcTemplate.queryForList("""
            SELECT cn.nspname as childSchema, c.relname AS childTable
            FROM pg_inherits
            JOIN pg_class AS c ON (inhrelid=c.oid)
            JOIN pg_catalog.pg_namespace cn ON (c.relnamespace=cn.oid)
            JOIN pg_class as p ON (inhparent=p.oid)
            JOIN pg_catalog.pg_namespace pn ON (p.relnamespace=pn.oid)
            WHERE pn.nspname || '.' || p.relname='${parentTable}';
        """)
        queryResult.collect { Map<String, Object> resultRow ->
            new Table(resultRow.childSchema.toString(), resultRow.childTable.toString())
        } as Set
    }

    void dropTable(Table tableToDrop, boolean ifExists = false) {
        jdbcTemplate.execute("DROP TABLE ${ifExists ? 'IF EXISTS' : ''} ${tableToDrop}")
    }

    Set<String> indexesForTable(Table table) {
        Set<String> indxNames = [] as Set
        ResultSet rs = connection.getMetaData().getIndexInfo(null, table.schema, table.name, false, false)
        try  {
            while (rs.next()) {
                indxNames << rs.getString('INDEX_NAME')
            }
        } finally {
            rs.close()
        }
        return indxNames
    }

    @Override
    void close() throws Exception {
        dataSource.close()
    }

}
