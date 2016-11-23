--
-- Name: i2b2_create_stage_tables(bigint); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_create_stage_tables(bigint) RETURNS integer
    LANGUAGE plpgsql IMMUTABLE SECURITY DEFINER
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
	newJobFlag 	int4;
	databaseName 	varchar(100);
	procedureName varchar(100);
	jobID 		numeric(18,0);
	stepCt 		numeric(18,0);
	rowCount	numeric(18,0);
	rtnCd integer;
	--	Define the abstract result set record
	
	r_stage_table		record;
	r_stage_column		record;
	
	--	Variables

	tText 			varchar(2000);
	pExists			int4;
	release_table	varchar(50);
	v_sqlerrm		varchar(1000);
	
	
	BEGIN	
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := -1;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_CREATE_STAGE_TABLES';

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		jobId := tm_cz.czx_start_audit (procedureName, databaseName);
	END IF;
    	
	stepCt := 0;	
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Starting ' || procedureName,0,stepCt,'Done')into rtnCd;
	
	for r_stage_table in
		select upper(table_owner) as table_owner
			  ,upper(table_name) as table_name
			  ,upper(study_specific) as study_specific
			  ,upper(stage_table_name) as stage_table_name
		from tm_cz.migrate_tables
	loop
		release_table := r_stage_table.stage_table_name;

		select count(*) into pExists
		from _v_table
		where schema = 'TM_STAGE'
		  and tablename = release_table;	
	  
		if pExists > 0 then
			tText := 'drop table tm_stage.' || release_table;
			execute immediate tText;
		end if;

		tText := 'create table tm_stage.' || release_table || ' (';
		
		for r_stage_column in
			select attname as column_name
				  ,format_type
			from _v_relation_column 
			where name=upper(r_stage_table.table_name)
			order by attnum asc
		loop
			tText := tText || r_stage_column.column_name || ' ' || r_stage_column.format_type || ',';	
		end loop;
		
		if r_stage_table.study_specific = 'Y' then
			tText := tText || 'release_study varchar(200))';
		else
			tText := trim(trailing ',' from tText) || ')';
		end if;
		execute immediate tText;
		stepCt := stepCt + 1;
		select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Created '|| release_table,0,stepCt,'Done')into rtnCd;
			
	end loop;
	
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'End i2b2_create_release_tablese',0,stepCt,'Done')into rtnCd;
	stepCt := stepCt + 1;
	
	    ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1
  THEN
    select tm_cz.czx_end_audit (jobID, 'SUCCESS')into rtnCd;
  END IF;

  EXCEPTION
  WHEN OTHERS THEN
	v_sqlerrm := substr(SQLERRM,1,1000);
	raise notice 'error: %', v_sqlerrm;
    --Handle errors.
    select tm_cz.czx_error_handler (jobID, procedureName,v_sqlerrm)into rtnCd;
    --End Proc
    select tm_cz.czx_end_audit (jobID, 'FAIL')into rtnCd;
	
END;
$$;

