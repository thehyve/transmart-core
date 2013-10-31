import groovy.sql.Sql
import DatabaseConnection

def runStoredProcedure() {
  sql = DatabaseConnection.setupDatabaseConnection()
  try {
    rows_changed = sql.call("call TM_CZ.i2b2_load_annotation_deapp()")
  } catch(SQLException e) {
    println e.getMessage()
    println "Call to load function failed; run showdblog target"
    System.exit 1
  }
  println "Rows changed ${rows_changed}"
  sql.close()
}

runStoredProcedure()

// vim: et sts=0 sw=2 ts=2 cindent cinoptions=(0,u0,U0
