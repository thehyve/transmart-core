--
-- Name: cz_error_handler(numeric, character varying, character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION cz_error_handler(jobid numeric, procedurename character varying, errornumber character varying, errormessage character varying) RETURNS integer
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
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
Declare
	databaseName VARCHAR(100);
	--errorNumber		character varying;	--	PostgreSQL SQLSTATE is alphanumeric
	--errorNumber NUMBER(18,0);
	--errorMessage VARCHAR(1000);
	errorStack VARCHAR(4000);
	errorBackTrace VARCHAR(4000);
	stepNo numeric(18,0);
	
	rtnCd	integer;

BEGIN
	--Get DB Name
	select database_name INTO databaseName
	from tm_cz.cz_job_master 
	where job_id=jobID;

	--Get Latest Step
	select max(step_number) into stepNo from tm_cz.cz_job_audit where job_id = jobID;
  
	--Get all error info, passed in as parameters, only available from EXCEPTION block
	--errorNumber := SQLSTATE;
	--errorMessage := SQLERRM;
	
	--	No corresponding functionality in PostgreSQL
	--errorStack := dbms_utility.format_error_stack;
	--errorBackTrace := dbms_utility.format_error_backtrace;

	--Update the audit step for the error
	select tm_cz.cz_write_audit(jobID, databaseName,procedureName, 'Job Failed: See error log for details',1, stepNo, 'FAIL') into rtnCd;
  
	--write out the error info
	select tm_cz.cz_write_error(jobID, errorNumber, errorMessage, errorStack, errorBackTrace) into rtnCd;
	
	return 1;

END;

$$;

