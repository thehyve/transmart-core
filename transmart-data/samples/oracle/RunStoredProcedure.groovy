/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-data.
 *
 * Transmart-data is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-data.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    this job. Job number found was ${job.job_id}
    """
    System.exit 1
  }
  sql.close()
}

runStoredProcedure()

// vim: et sts=0 sw=2 ts=2 cindent cinoptions=(0,u0,U0
