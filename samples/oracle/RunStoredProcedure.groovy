import groovy.sql.Sql
import DatabaseConnection

def runStoredProcedure() {
  sql = DatabaseConnection.setupDatabaseConnection()
  return_value = sql.execute("BEGIN TM_CZ.i2b2_load_annotation_deapp(); END;")
  if (return_value != 1) {
    println return_value
    println "Call to load function failed; run showdblog target"
    System.exit 1
  }
  sql.close()
}

runStoredProcedure()

// vim: et sts=0 sw=2 ts=2 cindent cinoptions=(0,u0,U0
