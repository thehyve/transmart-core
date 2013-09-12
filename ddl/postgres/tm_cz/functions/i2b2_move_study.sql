--
-- Name: i2b2_move_study(character varying, character varying, numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_move_study(old_path character varying, new_path character varying, currentjobid numeric) RETURNS integer
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

  root_node		varchar(2000);
  root_level	integer;
  rtnCd			integer;
  
	--Audit variables
	newJobFlag		integer;
	databaseName 	VARCHAR(100);
	procedureName 	VARCHAR(100);
	jobID 			numeric(18,0);
	stepCt 			numeric(18,0);
	rowCt			numeric(18,0);
	errorNumber		character varying;
	errorMessage	character varying;
  
BEGIN
    
	stepCt := 0;
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_ADD_NODE';
	
	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select tm_cz.cz_start_audit (procedureName, databaseName) into jobId;
	END IF;
  
	select tm_cz.parse_nth_value(path, 2, '\') into root_node;
	
	select c_hlevel into root_level
	from i2b2metadata.table_access
	where c_name = root_node;
  
	if old_path != ''  or old_path != '%' or new_path != ''  or new_path != '%'
	then 
	
      --CONCEPT DIMENSION
		update concept_dimension
		set CONCEPT_PATH = replace(concept_path, old_path, new_path)
		where concept_path like old_path || '%';
		get diagnostics rowCt := ROW_COUNT;
		stepCt := stepCt + 1;
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Update concept_dimension with new path',rowCt,stepCt,'Done') into rtnCd;




    
		--I2B2
		update i2b2
		set c_fullname = replace(c_fullname, old_path, new_path)
			,c_dimcode = replace(c_fullname, old_path, new_path)
			,c_tooltip = replace(c_fullname, old_path, new_path)
			,c_hlevel =  (length(replace(c_fullname, old_path, new_path)) - COALESCE(length(replace(replace(c_fullname, old_path, new_path), '\')),0)) / length('\') - 2 + root_level
		where c_fullname like old_path || '%';
		get diagnostics rowCt := ROW_COUNT;
		stepCt := stepCt + 1;
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Update i2b2 with new path',rowCt,stepCt,'Done') into rtnCd;



		
		--	concept_counts
		
		update concept_counts
		set concept_path = replace(concept_path, old_path, new_path)
		   ,parent_concept_path = replace(parent_concept_path, old_path, new_path)
		where concept_path like old_path || '%';
		
		--	fill in any upper levels
		
		select i2b2_fill_in_tree(null, new_path, jobID);
	END IF;



		
      ---Cleanup OVERALL JOB if this proc is being run standalone
	IF newJobFlag = 1
	THEN
		select tm_cz.cz_end_audit (jobID, 'SUCCESS') into rtnCD;
	END IF;

	return 1;
	
	EXCEPTION
	WHEN OTHERS THEN
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;

  
END;

$$;

