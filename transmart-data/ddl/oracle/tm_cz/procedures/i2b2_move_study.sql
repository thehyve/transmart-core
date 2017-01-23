--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_MOVE_STUDY
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_MOVE_STUDY" 
(
  old_path VARCHAR2,
  new_path VARCHAR2,
 -- topNode	varchar2,
  currentJobID NUMBER := null
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

  root_node		varchar2(2000);
  root_level	int;
  topLevel		int;
  newPath		varchar2(2000);
  pExists		int;
 
  --Audit variables
  newJobFlag INTEGER(1);
  databaseName VARCHAR(100);
  procedureName VARCHAR(100);
  jobID number(18,0);
  stepCt number(18,0);
  
  invalid_topNode	exception;
  
BEGIN

	stepCt := 0;
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	SELECT sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName FROM dual;
	procedureName := $$PLSQL_UNIT;
  
	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
	cz_start_audit (procedureName, databaseName, jobID);
	END IF;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Start i2b2_move_node',0,stepCt,'Done');  
	
	newPath := REGEXP_REPLACE('\' || new_path || '\','(\\){2,}', '\');
	select length(newPath)-length(replace(newPath,'\','')) into topLevel from dual;
	
	if topLevel < 3 then
		raise invalid_topNode;
	end if;
	
	select parse_nth_value(newPath, 2, '\') into root_node from dual;
	
	select count(*) into pExists
	from table_access
	where c_name = root_node;
	
	if pExists = 0 then
		i2b2_add_root_node(root_node,jobId);
	end if;
	
	select c_hlevel into root_level
	from table_access
	where c_name = root_node;
	
	if coalesce(old_path,'') = ''  or old_path = '%' or coalesce(new_path,'') = ''  or new_path = '%'
	then 
		cz_write_audit(jobId,databaseName,procedureName,'Old or new path missing or invalid',SQL%ROWCOUNT,stepCt,'Done'); 
	else
      --CONCEPT DIMENSION
		update concept_dimension
		set CONCEPT_PATH = replace(concept_path, old_path, newPath)
		where concept_path like old_path || '%';
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Update concept_dimension with new path',SQL%ROWCOUNT,stepCt,'Done'); 
		COMMIT;
    
		--I2B2
		update i2b2
		set c_fullname = replace(c_fullname, old_path, newPath)
			,c_dimcode = replace(c_fullname, old_path, newPath)
			,c_tooltip = replace(c_fullname, old_path, newPath)
			,c_hlevel =  (length(replace(c_fullname, old_path, newPath)) - nvl(length(replace(replace(c_fullname, old_path, new_path), '\')),0)) / length('\') - 2 + root_level
			,c_name = parse_nth_value(replace(c_fullname, old_path, newPath),(length(replace(c_fullname, old_path, newPath))-length(replace(replace(c_fullname, old_path, newPath),'\',null))),'\') 
		where c_fullname like old_path || '%';
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Update i2b2 with new path',SQL%ROWCOUNT,stepCt,'Done'); 
		COMMIT;
		
		--	concept_counts
		
		update concept_counts
		set concept_path = replace(concept_path, old_path, newPath)
		   ,parent_concept_path = replace(parent_concept_path, old_path, newPath)
		where concept_path like old_path || '%';
		
		--	update parent_concept_path for new_path (replace doesn't work)
		update concept_counts 
		set parent_concept_path=ltrim(SUBSTR(concept_path, 1,instr(concept_path, '\',-1,2)))
		where concept_path = newPath;
		
		--	fill in any upper levels
		
		i2b2_fill_in_tree(null, newPath, jobID);
		
/*
		--update level data
		UPDATE I2B2
		set c_hlevel = (length(c_fullname) - nvl(length(replace(c_fullname, '\')),0)) / length('\') - 2 + root_level
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

		if topNode != '' then
			i2b2_fill_in_tree
			i2b2_create_concept_counts(topNode,jobId);
		end if;
*/
	end if;
	
	i2b2_load_security_data(jobId);
	
	IF newJobFlag = 1
	THEN
		cz_end_audit (jobID, 'SUCCESS');
	END IF;

	EXCEPTION
	when invalid_topNode then
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Path specified in top_node must contain at least 2 nodes',0,stepCt,'Done');	
		cz_error_handler (jobID, procedureName);
		cz_end_audit (jobID, 'FAIL');
		--rtnCode := 16;
	WHEN OTHERS THEN
		--Handle errors.
		cz_error_handler (jobID, procedureName);
		--End Proc
		cz_end_audit (jobID, 'FAIL');
		
END;
 
/
 
