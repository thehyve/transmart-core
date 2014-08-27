--
-- Type: PROCEDURE; Owner: TM_CZ; Name: CZ_WRITE_AUDIT
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."CZ_WRITE_AUDIT" 
(
	jobId IN NUMBER,
	databaseName IN VARCHAR2 , 
	procedureName IN VARCHAR2 , 
	stepDesc IN VARCHAR2 , 
	recordsManipulated IN NUMBER,
	stepNumber IN NUMBER,
	stepStatus IN VARCHAR2
)
AS
/*************************************************************************
* Copyright 2008-2012 Janssen Research & Development, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/

  lastTime timestamp;
BEGIN
  select max(job_date)
    into lastTime
    from cz_job_audit
    where job_id = jobID;

	insert 	into cz_job_audit(
		job_id, 
		database_name,
 		procedure_name, 
 		step_desc, 
		records_manipulated,
		step_number,
		step_status,
    job_date,
    time_elapsed_secs
	)
	select
 		jobId,
		substr(databaseName, 1, 50),
		procedureName,
		stepDesc,
		recordsManipulated,
		stepNumber,
		stepStatus,
    SYSTIMESTAMP,
      COALESCE(
      EXTRACT (DAY    FROM (SYSTIMESTAMP - lastTime))*24*60*60 + 
      EXTRACT (HOUR   FROM (SYSTIMESTAMP - lastTime))*60*60 + 
      EXTRACT (MINUTE FROM (SYSTIMESTAMP - lastTime))*60 + 
      EXTRACT (SECOND FROM (SYSTIMESTAMP - lastTime))
      ,0)
  from dual;
  
  COMMIT;

END;
/
 
