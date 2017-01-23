--
-- Name: czx_end_audit(numeric, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION czx_end_audit(jobid numeric, jobstatus character varying) RETURNS numeric
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

declare
	endDate timestamp;
	rtnCd	numeric;

BEGIN
  
	select clock_timestamp() into endDate;
  
	begin
	update tm_cz.cz_job_master
		set 
			active='N',
			end_date = endDate,
			time_elapsed_secs = coalesce(((DATE_PART('day', endDate - START_DATE) * 24 + 
				   DATE_PART('hour', endDate - START_DATE)) * 60 +
				   DATE_PART('minute', endDate - START_DATE)) * 60 +
				   DATE_PART('second', endDate - START_DATE),0),
			job_status = jobStatus		
		where active='Y' 
		and job_id=jobID;
	end;
	
	return 1;
	
	exception 
	when OTHERS then
		--raise notice 'proc failed state=%  errm=%', SQLSTATE, SQLERRM;
		select tm_cz.cz_write_error(jobId,SQLSTATE,SQLERRM,null,null) into rtnCd;
		return -16;
END;
$$;

