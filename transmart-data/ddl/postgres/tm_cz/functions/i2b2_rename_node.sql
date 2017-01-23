--
-- Name: i2b2_rename_node(character varying, character varying, character varying, numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_rename_node(trial_id character varying, old_node character varying, new_node character varying, currentjobid numeric DEFAULT (-1)) RETURNS void
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
   
  --Audit variables
  newJobFlag integer(1);
  databaseName varchar(100);
  procedureName varchar(100);
  jobID bigint;
  stepCt bigint;
  

BEGIN
	
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
    	
  stepCt := 0;

  
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Start i2b2_rename_node',0,stepCt,'Done'); 
	
  if old_node != ''  and old_node != '%' and new_node != ''  and new_node != '%'
  then

	--	Update concept_counts paths

	update concept_counts cc
      set CONCEPT_PATH = replace(cc.concept_path, '\' || old_node || '\', '\' || new_node || '\'),
	      parent_concept_path = replace(cc.parent_concept_path, '\' || old_node || '\', '\' || new_node || '\')
      where cc.concept_path in
		   (select cd.concept_path from concept_dimension cd
		    where cd.sourcesystem_cd = trial_id
              and cd.concept_path like '%' || old_node || '%');
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Update concept_counts with new path',SQL%ROWCOUNT,stepCt,'Done'); 

    COMMIT;
	
    --Update path in i2b2_tags
    update i2b2_tags t
      set path = replace(t.path, '\' || old_node || '\', '\' || new_node || '\')
      where t.path in
		   (select cd.concept_path from concept_dimension cd
		    where cd.sourcesystem_cd = trial_id
              and cd.concept_path like '%\' || old_node || '\%');
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Update i2b2_tags with new path',SQL%ROWCOUNT,stepCt,'Done'); 

    COMMIT;
	
    --Update specific name
    --update concept_dimension
    --  set name_char = new_node
    --  where name_char = old_node
    --    and sourcesystem_cd = trial_id;

    --Update all paths
    update concept_dimension
      set CONCEPT_PATH = replace(concept_path, '\' || old_node || '\', '\' || new_node || '\')
	     ,name_char=(CASE WHEN name_char=old_node THEN new_node ELSE name_char END)
      where
		sourcesystem_cd = trial_id
        and concept_path like '%\' || old_node || '\%';
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Update concept_dimension with new path',SQL%ROWCOUNT,stepCt,'Done'); 

    COMMIT;



    --I2B2
    --Update specific name
    --update i2b2
    --  set c_name = new_node
    --  where c_name = old_node
    --    and c_fullname like '%' || trial_id || '%';

    --Update all paths, added updates to c_dimcode and c_tooltip instead of separate pass
    update i2b2
      set c_fullname = replace(c_fullname, '\' || old_node || '\', '\' || new_node || '\')
	  	 ,c_dimcode = replace(c_dimcode, '\' || old_node || '\', '\' || new_node || '\')
		 ,c_tooltip = replace(c_tooltip, '\' || old_node || '\', '\' || new_node || '\')
		 ,c_name = (CASE WHEN c_name=old_node THEN new_node ELSE c_name END)
      where sourcesystem_cd = trial_id
        and c_fullname like '%\' || old_node || '\%';
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Update i2b2 with new path',SQL%ROWCOUNT,stepCt,'Done'); 

    COMMIT;

	--Update i2b2_secure to match i2b2
    --update i2b2_secure
    --  set c_fullname = replace(c_fullname, old_node, new_node)
	--  	 ,c_dimcode = replace(c_dimcode, old_node, new_node)
	--	 ,c_tooltip = replace(c_tooltip, old_node, new_node)
    --  where
    --    c_fullname like '%' || trial_id || '%';
    --COMMIT;
	
	i2b2_load_security_data(jobID);


  END IF;
END;
 
$_$;

