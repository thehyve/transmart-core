--
-- Name: i2b2_move_node(character varying, character varying, character varying, numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_move_node(old_path character varying, new_path character varying, topnode character varying, currentjobid numeric DEFAULT (-1)) RETURNS void
    LANGUAGE plpgsql
    AS $_$
DECLARE

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

  root_node		varchar(2000);
  root_level	integer;
 
  --Audit variables
  newJobFlag integer(1);
  databaseName varchar(100);
  procedureName varchar(100);
  jobID numeric;
  stepCt integer;
  

BEGIN
  
	stepCt := 0;
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	PERFORM sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName ;
	procedureName := $$PLSQL_UNIT;
  
	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(coalesce(jobID::text, '') = '' or jobID < 1)
	THEN
		newJobFlag := 1; -- True
	cz_start_audit (procedureName, databaseName, jobID);
	END IF;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Start i2b2_move_node',0,stepCt,'Done');  
	
	PERFORM parse_nth_value(topNode, 2, '\') into root_node ;
	
	select c_hlevel into root_level
	from table_access
	where c_name = root_node;
	
	if old_path != ''  or old_path != '%' or new_path != ''  or new_path != '%'
	then 
      --CONCEPT DIMENSION
		update concept_dimension
		set CONCEPT_PATH = replace(concept_path, old_path, new_path)
		where concept_path like old_path || '%';
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Update concept_dimension with new path',SQL%ROWCOUNT,stepCt,'Done'); 
		COMMIT;
    
		--I2B2
		update i2b2
		set c_fullname = replace(c_fullname, old_path, new_path)
		where c_fullname like old_path || '%';
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Update i2b2 with new path',SQL%ROWCOUNT,stepCt,'Done'); 
		COMMIT;
  
		--update level data
		UPDATE I2B2
		set c_hlevel = (length(c_fullname) - coalesce(length(replace(c_fullname, '\')),0)) / length('\') - 2 + root_level
		where c_fullname like new_path || '%';
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Update i2b2 with new level',SQL%ROWCOUNT,stepCt,'Done'); 
		COMMIT;
		
		--Update tooltip and dimcode
		update i2b2
		set c_dimcode = c_fullname,
		c_tooltip = c_fullname
		where c_fullname like new_path || '%';
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Update i2b2 with new dimcode and tooltip',SQL%ROWCOUNT,stepCt,'Done'); 
		COMMIT;

		--if topNode != '' then
		--	i2b2_create_concept_counts(topNode,jobId);
		--end if;
	end if;
	
	IF newJobFlag = 1
	THEN
		cz_end_audit (jobID, 'SUCCESS');
	END IF;

	EXCEPTION
	WHEN OTHERS THEN
		--Handle errors.
		cz_error_handler (jobID, procedureName);
		--End Proc
		cz_end_audit (jobID, 'FAIL');
		
END;
 
$_$;

