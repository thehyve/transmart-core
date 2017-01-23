--
-- Name: czx_write_error(numeric, character varying, character varying, character varying, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION czx_write_error(jobid numeric, errornumber character varying, errormessage character varying, errorstack character varying, errorbacktrace character varying) RETURNS numeric
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

BEGIN

	begin
	insert into tm_cz.cz_job_error(
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
  from tm_cz.cz_job_audit 
  where job_id=jobID;
  
  end;
  
  return 1;
 
  exception 
	when OTHERS then
		raise notice 'proc failed state=%  errm=%', SQLSTATE, SQLERRM;
		return -16; 

END;
$$;

