import groovy.sql.Sql
import DatabaseConnection

def runStoredProcedure() {
  sql = DatabaseConnection.setupDatabaseConnection()
  sql.call("call TM_CZ.i2b2_load_annotation_deapp()")
  job = sql.firstRow("select * from TM_CZ.cz_job_master where job_id = (select max(job_id) FROM TM_CZ.cz_job_master)")
  if (job.job_status != "SUCCESS") {
    println "Call to load function failed; run showdblog target"
    println """
    Please note that this failure might very well be a false positive
    as the script _assumes_ the latest entry in the audit table is the one from
    this job.
    """
    System.exit 1
  }
  sql.close()
}

runStoredProcedure()

// vim: et sts=0 sw=2 ts=2 cindent cinoptions=(0,u0,U0
