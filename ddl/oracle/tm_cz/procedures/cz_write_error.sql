--
-- Type: PROCEDURE; Owner: TM_CZ; Name: CZ_WRITE_ERROR
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."CZ_WRITE_ERROR" 
(
	jobId IN NUMBER,
	errorNumber IN NUMBER , 
	errorMessage IN VARCHAR2 , 
	errorStack IN VARCHAR2,
  errorBackTrace IN VARCHAR2
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

	insert into cz_job_error(
		job_id,
		error_number,
		error_message,
		error_stack,
    error_backtrace,
		seq_id)
	select
		jobID,
		errorNumber,
		errorMessage,
		errorStack,
    errorBackTrace,
		max(seq_id) 
  from 
    cz_job_audit 
  where 
    job_id=jobID;
  
  COMMIT;

END;
/
 
