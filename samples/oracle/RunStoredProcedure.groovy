import groovy.sql.Sql

def setupDatabaseConnection() {
  def driver = "oracle.jdbc.driver.OracleDriver"
  def jdbcUrl = "jdbc:oracle:thin:@${System.getenv('ORAHOST')}:${System.getenv('ORAPORT')}:${System.getenv('ORASID')}"
  def username = 'tm_cz'
  def password = 'tm_cz'
  Sql sql = Sql.newInstance jdbcUrl, username, password, driver
  sql
}

def runStoredProcedure() {
  sql = setupDatabaseConnection()
  return_value = sql.execute("SELECT TM_CZ.i2b2_load_annotation_deapp()")
  if (return_value != 1) {
    println return_value
    println "Call to load function failed; check error/audit tables"
  }
  sql.close()
}
