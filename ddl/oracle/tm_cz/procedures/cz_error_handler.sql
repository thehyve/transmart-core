--
-- Type: PROCEDURE; Owner: TM_CZ; Name: CZ_ERROR_HANDLER
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."CZ_ERROR_HANDLER" 
(
  jobID NUMBER,
  procedureName NVARCHAR2
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

  databaseName NVARCHAR2(100);
	errorNumber NUMBER(18,0);
	errorMessage VARCHAR2(1000);
  errorStack VARCHAR2(4000);
  errorBackTrace VARCHAR2(4000);
	stepNo NUMBER(18,0);

BEGIN
  --Get DB Name
	select database_name INTO databaseName
		from cz_job_master 
		where job_id=jobID;
  --Get Latest Step
	select max(step_number) into stepNo from cz_job_audit where job_id = jobID;
  
  --Get all error info
  errorNumber := SQLCODE;
  errorMessage := SQLERRM;
  errorStack := dbms_utility.format_error_stack;
  errorBackTrace := dbms_utility.format_error_backtrace;

  --Update the audit step for the error
  cz_write_audit(jobID, databaseName,procedureName, 'Job Failed: See error log for details',SQL%ROWCOUNT, stepNo, 'FAIL');

  
  --write out the error info
  cz_write_error(jobID, errorNumber, errorMessage, errorStack, errorBackTrace);

END;
/
 
