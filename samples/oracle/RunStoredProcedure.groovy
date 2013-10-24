import groovy.sql.Sql
import DatabaseConnection

def runStoredProcedure() {
  sql = DatabaseConnection.setupDatabaseConnection()
  return_value = sql.execute("SELECT TM_CZ.i2b2_load_annotation_deapp()")
  if (return_value != 1) {
    println return_value
    println "Call to load function failed; check error/audit tables"
  }
  sql.close()
}

runStoredProcedure()
