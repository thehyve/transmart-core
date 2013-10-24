@Grab(group='net.sf.opencsv', module='opencsv', version='2.3')
import au.com.bytecode.opencsv.CSVReader
import DatabaseConnection
import groovy.sql.Sql
import groovy.sql.BatchingPreparedStatementWrapper

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

def uploadTsvFileToTable(Sql sql, File file, String table, String columns, int batchSize) {
  CSVReader reader       = new CSVReader(file.newReader('UTF-8'), '\t' as char)
  int       i            = 0
  String    colGroup     = ''
  String    placeHolders
  String[]  line

  if (columns) {
    colGroup = columns.split(',').collect({ String it ->
      it[0] == '"' ?
        it :
        ('"' + it.toUpperCase(Locale.ENGLISH) + '"')
    }).join(', ')
    colGroup = "($colGroup)"
  }

  line = reader.readNext()

  if (!line) {
    return
  }

  placeHolders = line.collect({ '?' }).join(', ')

  sql.withBatch batchSize, "INSERT INTO $table$colGroup VALUES ($placeHolders)", {
    BatchingPreparedStatementWrapper it ->
    while (line != null) {
      i++
      if (i % 10000 == 0) {
        println i
      }

      it.addBatch line

      line = reader.readNext()
    }
  }
  println i
}

def truncateTable(sql, table) {
  sql.execute("TRUNCATE TABLE $table" as String)
}

options = parseOptions()

if (!options) {
	System.exit 1
}

def sql = DatabaseConnection.setupDatabaseConnection()

sql.withTransaction {
  if (options.truncate) {
    print "Truncating table ${options.table}... "
    truncateTable(sql, options.table)
    println 'Done'
  }

  uploadTsvFileToTable(sql,
                       options.file ? new File(options.file) : System.in,
                       options.table,
                       options.c,
                       options.b ? options.b as int : 5000)
}

// vim: et sts=0 sw=2 ts=2 cindent cinoptions=(0,u0,U0
