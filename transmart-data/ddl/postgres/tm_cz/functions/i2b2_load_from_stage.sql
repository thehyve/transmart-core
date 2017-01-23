--
-- Name: i2b2_load_from_stage(character varying, character varying, bigint); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_load_from_stage(character varying, character varying, bigint) RETURNS integer
    LANGUAGE plpgsql IMMUTABLE SECURITY DEFINER
    AS $_$
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

	--	Alias for parameters
	
	trial_id  alias for $1;
	data_type alias for $2;
	currentJobID alias for $3;
	rtnCd integer;
	TrialId 		varchar(200);
	msgText			varchar(2000);
	dataType		varchar(50);

	tText			varchar(2000);
	tExists 		integer;
	source_table	varchar(50);
	release_table	varchar(50);
	tableOwner		varchar(50);
	tableName		varchar(50);
	vSNP 			integer;
	topNode			varchar(1000);
	rootNode		varchar(1000);
	tPath			varchar(1000);
	pExists			integer;
	pCount			integer;
	rowCt			bigint;
	bslash			char(1);

	--Audit variables
	newJobFlag integer;
	databaseName VARCHAR(100);
	procedureName VARCHAR(100);
	jobID numeric(18,0);
	stepCt numeric(18,0);
	v_sqlerrm		varchar(1000);
  
	r_stage_table	record;
	r_stage_columns record;
	
BEGIN

	TrialID := upper(trial_id);
	dataType := upper(data_type);
	
	stepCt := 0;
	pCount := 0;
	bslash := '\\';
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_LOAD_FROM_STAGE';

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		jobId := tm_cz.czx_start_audit (procedureName, databaseName);
	END IF;
  
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Starting ' || procedureName,0,stepCt,'Done')into rtnCd;
	
	stepCt := stepCt + 1;
	msgText := 'Extracting trial: ' || TrialId;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName, msgText,0,stepCt,'Done');

	if TrialId is null then
		stepCt := stepCt + 1;
		select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'TrialID missing',0,stepCt,'Done')into rtnCd;
		return 16;
	end if;
	
	for r_stage_table in
		select upper(table_owner) as table_owner
			  ,upper(table_name) as table_name
			  ,upper(study_specific) as study_specific
			  ,where_clause
			  ,upper(stage_table_name) as stage_table_name
		from tm_cz.migrate_tables
		where instr(dataType,data_type) > 0
	loop	
		pCount := pCount + 1;
		--	setup variables
		
		source_table := r_stage_table.table_owner || '.' || r_stage_table.table_name;
		release_table := 'tm_stage.' || r_stage_table.stage_table_name;
		tableName := r_stage_table.table_name;
		tableOwner := r_stage_table.table_owner;
		stepCt := stepCt + 1;
		select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Processing ' || source_table,0,stepCt,'Done')into rtnCd;

		if r_stage_table.study_specific = 'N' then	
			--	truncate target table
			tText := 'truncate table ' || source_table;
			execute immediate tText;
			stepCt := stepCt + 1;		
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Truncated '|| source_table,0,stepCt,'Done')into rtnCd;
			--	insert from staged source into target
			tText := 'insert into ' || source_table || ' select st.* from ' || release_table || ' st ';
			execute immediate tText;
			rowCt := ROW_COUNT;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Inserted all data into ' || source_table,rowCt,stepCt,'Done')into rtnCd;		
		else
			tText := 'delete from ' || source_table || ' st ' || replace(r_stage_table.where_clause,'TrialId','''' || TrialId || '''');
			execute immediate tText;
			rowCt := ROW_COUNT;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Deleted study from ' || source_table,ROW_COUNT,stepCt,'Done')into rtnCd;
			tText := 'insert into ' || source_table || ' select ';
			
			--	get list of columns in order
		
			for r_stage_columns in
				select attname as column_name
				from _v_relation_column 
				where name=upper(tablename)
				order by attnum asc
			loop
				--	insert by column for study_specific 
				tText := tText || ' st.' || r_stage_columns.column_name || ',';
			end loop;
			
			tText := trim(trailing ',' from tText) || ' from ' || release_table || ' st ' || ' where st.release_study = ' || '''' || TrialId || '''';
			execute immediate tText;
			rowCt := ROW_COUNT;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Inserted study into ' || source_table,rowCt,stepCt,'Done')into rtnCd;
		end if;		
		
	end loop;
	
	if pCount = 0 then
		stepCt := stepCt + 1;
		select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'No staged data for study',0,stepCt,'Done')into rtnCd;
		return 16;
	end if;	

	--	if CLINICAL data, add root node if needed and fill in tree for any top nodes
	
	if instr(dataType,'CLINICAL') > 0 then
	
		--	get topNode for study
	
		select min(c_fullname) into topNode
		from i2b2metadata.i2b2
		where sourcesystem_cd = TrialId;
		
		if topNode is null then
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Unable to get topNode for study',0,stepCt,'Done')into rtnCd;
			return 16;
		end if;
		
		-- Get rootNode from topNode
  
		rootNode := replace(substr(topNode,1,instr(topNode,bslash,2)),bslash,'');
	
		select count(*) into pExists
		from i2b2metadata.table_access
		where c_name = rootNode;
	
		select count(*) into pCount
		from i2b2metadata.i2b2
		where c_name = rootNode;
	
		if pExists = 0 or pCount = 0 then
			select tm_cz.i2b2_add_root_node(rootNode, jobId)into rtnCd;
		end if;
		
		--	Add any upper level nodes as needed, trim off study name because it's already in i2b2
	
		tPath := substr(topNode, 1,instr(topNode,bslash,-2,1));
		pCount := length(tPath) - length(replace(tPath,bslash,''));

		if pCount > 2 then
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Adding upper-level nodes',0,stepCt,'Done')into rtnCd;
			select i2b2_fill_in_tree(null, tPath, jobId)into rtnCd;
		end if;

		select tm_cz.i2b2_load_security_data(jobId)into rtnCd;
	end if;

	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'End '||procedureName,0,stepCt,'Done')into rtnCd;

       ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1
  THEN
    select tm_cz.czx_end_audit (jobID, 'SUCCESS')into rtnCd;
  END IF;
  
  return 0;

  EXCEPTION
  WHEN OTHERS THEN
	v_sqlerrm := substr(SQLERRM,1,1000);
	raise notice 'error: %', v_sqlerrm;
    --Handle errors.
    select tm_cz.czx_error_handler (jobID, procedureName,v_sqlerrm)into rtnCd;
    --End Proc
    select tm_cz.czx_end_audit (jobID, 'FAIL')into rtnCd;
	return 16;
END;

$_$;

