--
-- Type: PROCEDURE; Owner: TM_CZ; Name: CZ_START_AUDIT
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."CZ_START_AUDIT" 
(
  jobName IN VARCHAR2,
  databaseName IN VARCHAR2,
  jobID OUT NUMBER
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
BEGIN

	insert into cz_job_master
		(start_date, 
		active, 
		--username,
		--session_id, 
		database_name,
		job_name,
		job_status) 
	VALUES(
		SYSTIMESTAMP,
		'Y', 
		--suser_name(),
		--@@SPID, 
		databaseName,
		jobName,
		'Running')
  RETURNING job_id INTO jobID;

  COMMIT;

END;
/
 
