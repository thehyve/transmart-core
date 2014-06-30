--
-- Name: cz_end_audit(numeric, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION cz_end_audit(jobid numeric, jobstatus character varying) RETURNS numeric
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
declare
	endDate timestamp;
	rtnCd	numeric;

BEGIN
  
	select current_timestamp into endDate;
  
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

