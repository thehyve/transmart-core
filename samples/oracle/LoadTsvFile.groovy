import DatabaseConnection
import au.com.bytecode.opencsv.CSVParser
@Grab(group = 'net.sf.opencsv', module = 'opencsv', version = '2.3')
import au.com.bytecode.opencsv.CSVReader
import groovy.sql.BatchingPreparedStatementWrapper
import groovy.sql.Sql

import java.sql.SQLException
import java.util.regex.Pattern

def parseOptions() {
    def cli = new CliBuilder(usage: "LoadTsvFile.groovy")
    cli.t 'qualified table name', required: true, longOpt: 'table', args: 1, argName: 'table'
    cli.f 'tsv file; stdin if unspecified', longOpt: 'file', args: 1, argName: 'file'
    cli.c 'column names', longOpt: 'cols', argName: 'col1,col2,...', args: 1
    cli._ 'truncate table before', longOpt: 'truncate'
    cli.b 'batch size', longOpt: 'batch', args: 1
    def options = cli.parse(args)
    if (options && options.b && (!options.b.isInteger() || options.b < 0)) {
        System.err.println 'Bad value for batch size'
        return false
    }
    options
}

private String constructColumnExpression(List<String> columns) {
    columns.collect({ column ->
        if (column ==~ '".+"') {
            column
        } else {
            '"' + column.toUpperCase(Locale.ENGLISH) + '"'
        }
    }).join(', ')
}

private String constructPlaceHoldersExpression(Map<String, String> columnTypeMap) {
    columnTypeMap.collect({ entry ->
        '?'
    }).join(', ')
}

def uploadTsvFileToTable(Sql sql, InputStream istr, String table, String csColumns, int batchSize) {
    CSVReader reader = new CSVReader(new InputStreamReader(istr, 'UTF-8'),
                                     '\t' as char,
                                     CSVParser.DEFAULT_QUOTE_CHARACTER,
                                     CSVParser.NULL_CHARACTER)

    String[] line = reader.readNext()
    if (!line) {
        return
    }

    List<String> columns = csColumns ? csColumns.split(',') as List : []

    def columnsInfo = getColumnTypesMap(sql, table, columns)

    if (columnsInfo.size() != line.size()) {
        throw new Exception("Expected to insert into ${columnsInfo.size()} " +
                "columns ${columnsInfo.keySet()}, but first line of TSV file " +
                "has ${line.size()} ($line)")
    }

    String colGroup = constructColumnExpression(columnsInfo.keySet() as List)
    String placeHolders = constructPlaceHoldersExpression(columnsInfo)
    Closure<String[]> dataFilter = constructDataFilter(columnsInfo)

    int i = 0
    int colsNum = line.length
    sql.withBatch batchSize, "INSERT INTO ${table}(${colGroup}) VALUES (${placeHolders})", {
        BatchingPreparedStatementWrapper it ->
            while (line != null) {
                i++
                if (i % 10000 == 0) {
                    print '.'
                }

                if (line.length != colsNum) {
                    System.err "${i} line contains wrong number of columns " +
                            "(${line.length} but expected to be ${colsNum}): ${line}; " +
                            "continuing anyway"
                } else {
                    it.addBatch dataFilter(line)
                }

                line = reader.readNext()
            }
    }
    reader.close()
    println " Done after $i rows"
}

private Closure<String[]> constructDataFilter(Map<String, ColumnMeta> columnsInfo) {
    List booleanColumnsPositions = columnsInfo.findIndexValues { entry ->
        entry.value.type == 'NUMBER' && entry.value.size == 1
    }.collect { it as int }
    List datePositions = columnsInfo.findIndexValues { entry ->
        entry.value.type == 'DATE'
    }.collect { it as int }
    Pattern cutMillis = ~/\.\d{3,6}$/

    return { String[] line ->
        booleanColumnsPositions.each { pos ->
            switch (line[pos]) {
                case 't':
                    line[pos] = '1'
                    break
                case 'f':
                    line[pos] = '0'
                    break
            }
        }

        /* DATE in Oracle was mapped to TIMESTAMP in PostgreSQL. However,
          TIMESTAMP has microsecond precision in PostgreSQL and DATE has
          second precision. We need to remove the fractional second part
          from the value, otherwise Oracle will reject it. */
        datePositions.each { pos ->
            def matcher = cutMillis.matcher(line[pos])
            line[pos] = matcher.replaceFirst('')
        }

        line /* also changes the argument! */
    }
}

@groovy.transform.Immutable
class ColumnMeta {
    String type
    Integer size
}

Map<String, ColumnMeta> getColumnTypesMap(Sql sql, String table, List<String> columns = []) {
    def columnTypes = [:]
    def sqlExpression = "SELECT ${columns ? constructColumnExpression(columns) : '*'} FROM ${table}".toString()
    sql.rows sqlExpression, 1, 0, { meta ->
        columnTypes = (1..meta.columnCount).collectEntries {
            [(meta.getColumnLabel(it)): new ColumnMeta(
                    type: meta.getColumnTypeName(it),
                    size: meta.getPrecision(it))]
        }
    }

    if (columns) {
        columns.collectEntries{
            def columnName = it.toUpperCase(Locale.ENGLISH)
            [(columnName): columnTypes[columnName]]
        }
    } else {
        columnTypes
    }
}

def truncateTable(sql, table) {
    try {
        print "Truncating table ${options.table}... "
        sql.execute("TRUNCATE TABLE $table" as String)
        println 'Done'
    } catch (SQLException sqlException) {
        if (sqlException.errorCode == 2266) {
            // cannot truncate due to foreign keys
            print 'Failed. Trying delete... '
            try {
                sql.execute("DELETE FROM $table" as String)
                println 'Done'
            } catch (SQLException e) {
                println 'Failed again! Aborting'
                throw e
            }
        } else {
            // failed for another reason
            println 'Failed! Aborting'
            throw sqlException
        }
    }
}

options = parseOptions()

if (!options) {
    System.exit 1
}

def sql = DatabaseConnection.setupDatabaseConnection()
if (System.getenv('NLS_DATE_FORMAT')) {
    sql.execute "ALTER SESSION SET NLS_DATE_FORMAT = '${System.getenv('NLS_DATE_FORMAT')}'".toString()
}
if (System.getenv('NLS_TIMESTAMP_FORMAT')) {
    sql.execute "ALTER SESSION SET NLS_TIMESTAMP_FORMAT = '${System.getenv('NLS_TIMESTAMP_FORMAT')}'".toString()
}
sql.withTransaction {
    if (options.truncate) {
        truncateTable(sql, options.table)
    }

    uploadTsvFileToTable(sql,
            options.file ? new FileInputStream(options.file) : System.in,
            options.table,
            options.c ?: '',
            options.b ? options.b as int : 5000)
}

// vim: et sts=0 sw=4 ts=4 cindent cinoptions=(0,u0,U0
