--
-- Name: i2b2_hide_node(character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_hide_node(path character varying) RETURNS numeric
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

	--Audit variables
	newJobFlag		integer;
	databaseName 	VARCHAR(100);
	procedureName 	VARCHAR(100);
	jobID 			numeric(18,0);
	stepCt 			numeric(18,0);
	rowCt			numeric(18,0);
	errorNumber		character varying;
	errorMessage	character varying;
	rtnCd			numeric;
	
begin

    stepCt := 0;
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'tm_cz';
	procedureName := 'i2b2_hide_node';

  --Audit JOB Initialization
  --If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select tm_cz.cz_start_audit (procedureName, databaseName) into jobID;
	END IF;
	
	if path = ''  or path = '%' or path_name = ''
	then 
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Path or Path name missing, no action taken',0,stepCt,'Done') into rtnCd;
		return 1;
	end if;

	begin
	update i2b2metadata.i2b2 b
	set c_visualattributes=substr(b.c_visualattributes,1,1) || 'H' || substr(b.c_visualattributes,3,1)
	where c_fullname like path || '%' escape '`';
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	get diagnostics rowCt := ROW_COUNT;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Nodes hidden in i2b2metadata.i2b2',rowCt,stepCt,'Done') into rtnCd;

	
	begin
	delete from i2b2demodata.concept_counts
	where concept_path like path || '%' escape '`';
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	get diagnostics rowCt := ROW_COUNT;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Deleted hidden nodes from i2b2demodata.concept_counts',rowCt,stepCt,'Done') into rtnCd;

	--	reload i2b2_secure for hidden nodes
	
	select tm_cz.load_security_data(jobId) into rtnCd;
	
	return 1;
  
END;

$$;

