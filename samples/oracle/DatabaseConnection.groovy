import groovy.sql.Sql

static def setupDatabaseConnection() {
  String driver = "oracle.jdbc.driver.OracleDriver"
  String jdbcUrl = "jdbc:oracle:thin:@${System.getenv('ORAHOST')}:${System.getenv('ORAPORT')}:${System.getenv('ORASID')}"
  String username = System.getenv('ORAUSER')
  String password = System.getenv('ORAPASSWORD')
  Sql sql = Sql.newInstance jdbcUrl, username, password, driver
  sql
}
