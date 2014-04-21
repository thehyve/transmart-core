import DatabaseConnection
import au.com.bytecode.opencsv.CSVParser
@Grab(group = 'net.sf.opencsv', module = 'opencsv', version = '2.3')
import au.com.bytecode.opencsv.CSVReader
import groovy.sql.BatchingPreparedStatementWrapper
import groovy.sql.Sql

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

private String constructColumnExpression(Map<String, String> columnTypeMap) {
    columnTypeMap.collect({ entry ->
        if (entry.key ==~ '".+"') {
            entry.key
        } else {
            '"' + entry.key.toUpperCase(Locale.ENGLISH) + '"'
        }
    }).join(', ')
}

private String constructPlaceHoldersExpression(Map<String, String> columnTypeMap) {
    columnTypeMap.collect({ entry ->
    	'?'
    }).join(', ')
}

def uploadTsvFileToTable(Sql sql, InputStream istr, String table, String csColumns, int batchSize) {
    CSVReader reader = new CSVReader(new InputStreamReader(istr, 'UTF-8'), '\t' as char, CSVParser.DEFAULT_QUOTE_CHARACTER, CSVParser.NULL_CHARACTER)

    String[] line = reader.readNext()
    if (!line) return

    List<String> columns = csColumns ? csColumns.split(',') as List : []

    def columnsInfo = getColumnTypesMap(sql, table, columns)

    String colGroup = constructColumnExpression(columnsInfo)
    String placeHolders = constructPlaceHoldersExpression(columnsInfo)
    def booleanCollumnsPositions = []
    columnsInfo.eachWithIndex { ci, index ->
        if(ci.value.type == 'NUMBER' && ci.value.size == 1) {
            booleanCollumnsPositions << index
        }
    }

    int i = 0
    int colsNum = line.length
    sql.withBatch batchSize, "INSERT INTO ${table}(${colGroup}) VALUES (${placeHolders})", {
        BatchingPreparedStatementWrapper it ->
            while (line != null) {
                i++
                if (i % 10000 == 0) {
                    println i
                }

                if (line.length != colsNum) {
                    println("${i} line contains wrong number of columns (${line.length} but expected to be ${colsNum}): ${line}")
                } else {
                    if(booleanCollumnsPositions) {
                        boolToNum(line, booleanCollumnsPositions)
                    }
                    it.addBatch line
                }

                line = reader.readNext()
            }
    }
    reader.close()
    println i
}
//TODO Find better way
private def boolToNum(line, booleanCollumnsPositions) {
    booleanCollumnsPositions.each { pos ->
        if(line[pos] == 't') {
            line[pos] = '1'
        } else if(line[pos] == 'f') {
            line[pos] = '0'
        }
    }
}

def getColumnTypesMap(Sql sql, String table, List<String> columns = []) {
    def columnTypes = [:]
    sql.rows "SELECT ${columns ? constructColumnExpression(columns) : '*'} FROM ${table}".toString(), 0, 0, { meta ->
        columnTypes = (1..meta.columnCount).collectEntries {
            [(meta.getColumnLabel(it)): [type: meta.getColumnTypeName(it), size: meta.getPrecision(it)]]
        }
    }
    if(columns) {
        columns.collectEntries{
            def columnName = it.toUpperCase(Locale.ENGLISH)
            [(columnName): columnTypes[columnName]]
        }
    } else {
        columnTypes
    }
}

def truncateTable(sql, table) {
    sql.execute("TRUNCATE TABLE $table" as String)
}

options = parseOptions()

if (!options) {
    System.exit 1
}

def sql = DatabaseConnection.setupDatabaseConnection()
if(System.getenv('NLS_DATE_FORMAT')) {
	sql.execute "ALTER SESSION SET NLS_DATE_FORMAT = '${ System.getenv('NLS_DATE_FORMAT') }'".toString()
}
if(System.getenv('NLS_TIMESTAMP_FORMAT')) {
	sql.execute "ALTER SESSION SET NLS_TIMESTAMP_FORMAT = '${ System.getenv('NLS_TIMESTAMP_FORMAT') }'".toString()
}
sql.withTransaction {
    if (options.truncate) {
        print "Truncating table ${options.table}... "
        truncateTable(sql, options.table)
        println 'Done'
    }

    uploadTsvFileToTable(sql,
            options.file ? new FileInputStream(options.file) : System.in,
            options.table,
            options.c ?: '',
            options.b ? options.b as int : 5000)
}

// vim: et sts=0 sw=2 ts=2 cindent cinoptions=(0,u0,U0
