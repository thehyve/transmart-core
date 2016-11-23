--
-- Name: i2b2_fill_in_tree(character varying, character varying, numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_fill_in_tree(trial_id character varying, path character varying, currentjobid numeric DEFAULT (-1)) RETURNS numeric
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
	
    TrialID varchar(100);
	auditText varchar(4000);
	root_node varchar(1000);
	node_name varchar(1000);
	v_count numeric;
  
  --Get the nodes
  cNodes CURSOR for
    --Trimming off the last node as it would never need to be added.
    select distinct substr(c_fullname, 1,instr(c_fullname,'\',-2,1)) as c_fullname
    from i2b2metadata.i2b2 
    where c_fullname like path || '%' escape '`';

  
BEGIN
  TrialID := upper(trial_id);
  
    stepCt := 0;
  --Set Audit Parameters
  newJobFlag := 0; -- False (Default)
  jobID := currentJobID;

  databaseName := 'TM_CZ';
  procedureName := 'I2B2_FILL_IN_TREE';

  --Audit JOB Initialization
  --If Job ID does not exist, then this is a single procedure run and we need to create it
  IF(jobID IS NULL or jobID < 1)
  THEN
    newJobFlag := 1; -- True
    select tm_cz.cz_start_audit (procedureName, databaseName) into jobID;
  END IF;
  
  --start node with the first slash

  --Iterate through each node
  FOR r_cNodes in cNodes Loop
    root_node := '\';
    --Determine how many nodes there are
    --Iterate through, Start with 2 as one will be null from the parser
    
    for loop_counter in 2 .. (length(r_cNodes.c_fullname) - coalesce(length(replace(r_cNodes.c_fullname, '\','')),0)) / length('\')
    LOOP
		--Determine Node:
		node_name := tm_cz.parse_nth_value(r_cNodes.c_fullname, loop_counter, '\');
		root_node :=  root_node || node_name || '\';
    
        --Check if node exists. If it does not, add it.
        select count(*) into v_count 
        from i2b2metadata.i2b2
        where c_fullname = root_node;

        --If it doesn't exist, add it
        if v_count = 0 then
			auditText := 'Inserting ' || root_node;
			stepCt := stepCt + 1;
			select tm_cz.cz_write_audit(jobId,databaseName,procedureName,auditText,0,stepCt,'Done') into rtnCD;
            select tm_cz.i2b2_add_node(trial_id, root_node, node_name, jobId) into rtnCd;
        end if;
      
    END LOOP;

    --RESET VARIABLES
    root_node := '';
    node_name := '';
  END LOOP;
  
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

