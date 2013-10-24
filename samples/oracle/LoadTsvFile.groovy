@Grab(group='net.sf.opencsv', module='opencsv', version='2.3')
import groovy.sql.Sql

def parseOptions() {
  def cli = new CliBuilder(usage: "load_tsv_file.groovy -t table -f file")
  cli.f('which file', required: true, longOpt: "file", args: 1)
  cli.t('which table', required: true, longOpt: "table", args: 1)
  def options = cli.parse(args)
  options
}

def setupDatabaseConnection() {
  def driver = "oracle.jdbc.driver.OracleDriver"
  def jdbcUrl = "jdbc:oracle:thin:@${System.getenv('ORAHOST')}:${System.getenv('ORAPORT')}:${System.getenv('ORASID')}"
  def username = 'tm_cz'
  def password = 'tm_cz'
  Sql sql = Sql.newInstance jdbcUrl, username, password, driver
  sql
}

def uploadTsvFileToTable(file, table) {
  sql = setupDatabaseConnection()
  sql.execute("LOAD DATA INFILE ${options.file} INTO TABLE ${options.table}")
}

def truncateTable(table) {
  sql = setupDatabaseConnection()
  sql.execute("TRUNCATE ${table}")
}

//def loadAnnotationParams() {
  //def annotationParams = new File('')
//}

//def uploadPlatformGplInfo() {
  //loadAnnotationParams()
  //sql = setupDatabaseConnection()
  //sql.
//}

options = parseOptions()
if (!options) { return }

truncateTable(options.table)
uploadTsvFileToTable(options.file, options.table)
