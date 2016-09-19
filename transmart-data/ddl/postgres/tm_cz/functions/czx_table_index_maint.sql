--
-- Name: czx_table_index_maint(character varying, character varying, character varying, numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION czx_table_index_maint(p_run_type character varying, p_schema character varying, p_table character varying DEFAULT 'ALL'::character varying, currentjobid numeric DEFAULT NULL::numeric) RETURNS bigint
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
/*
 *   Copyright 2012-2013 The Regents of the University of Colorado
 *
 *   Licensed under the Apache License, Version 2.0 (the "License")
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
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
	rtnCd			integer;
	
	v_ct		int;
	v_index		record;
	v_sql		varchar(4000);
  
begin

	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;
	databaseName := 'tm_cz';
	procedureName := 'czx_table_index_maint';

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it

	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select tm_cz.czx_start_audit (procedureName, databaseName) into jobID;
	END IF;

	stepCt := 0;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Starting ' || procedureName,0,stepCt,'Done') into rtnCd;
	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,' run_type: ' || p_run_type || ' schema: ' || p_schema ||  ' table: ' || p_table,0,stepCt,'Done') into rtnCd;


	if p_run_type not in ('DROP','ADD','SAVE') then
		stepCt := stepCt + 1;
		select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Invalid run_type',0,stepCt,'Done') into rtnCd;
		select tm_cz.czx_error_handler (jobID, procedureName, '-1', 'Application raised error') into rtnCd;
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return 16;
	end if;
	
	if p_schema is null or p_schema = '' then
		stepCt := stepCt + 1;
		select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Schema name missing',0,stepCt,'Done') into rtnCd;
		select tm_cz.czx_error_handler (jobID, procedureName, '-1', 'Application raised error') into rtnCd;
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return 16;
	end if;
	
	select count(*) into v_ct
	from tm_cz.table_index
	where schema_name = p_schema
	  and case when p_run_type = 'ALL' then 1
	           else case when table_name = p_table then 1
			             else 0
						 end
			   end = 1;
	
	if v_ct = 0 then
		if p_run_type != 'SAVE' then
			if p_table = 'ALL' then
				stepCt := stepCt + 1;
				select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'No saved indexes foound for schema',0,stepCt,'Done') into rtnCd;
			else
				stepCt := stepCt + 1;
				select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'No saved indexes found for table',0,stepCt,'Done') into rtnCd;
			end if;
			select tm_cz.czx_error_handler (jobID, procedureName, '-1', 'Application raised error') into rtnCd;
			select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
			return 16;
		end if;
	end if;
	
	--	SAVE indexes
	
	if p_run_type = 'SAVE' then
	
		--	check that indexes exist for supplied schema name
		
		select count(*) into v_ct
		from pg_class a
		inner join pg_index b
			  on  a.oid = b.indexrelid 
		inner join pg_class c
			  on  b.indrelid = c.oid
		inner join pg_attribute d
			  on  c.oid = d.attrelid 
			  and d.attnum = any(b.indkey)
		inner join pg_namespace n
			  on n.oid = a.relnamespace
		where a.relname not like 'pg_%'
		  and n.nspname = p_schema
		  and case when p_table = 'ALL' then 1
				   else case when p_table = c.relname then 1
							 else 0 end
					end = 1;
	
	if v_ct = 0 then
		if p_table = 'ALL' then
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'No indexes foound for schema',0,stepCt,'Done') into rtnCd;
		else
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'No indexes found for table',0,stepCt,'Done') into rtnCd;
		end if;
		select tm_cz.czx_error_handler (jobID, procedureName, '-1', 'Application raised error') into rtnCd;
		select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
		return 16;
	end if;
	
		--	delete existing indexes for schema tables from cz.table_maint
		
		begin
		delete from tm_cz.table_index
		where schema_name = p_schema
		  and case when p_table = 'ALL' then 1
				   else case when p_table = table_name then 1
							 else 0 end
				   end = 1;
		get diagnostics rowCt := ROW_COUNT;
		exception 
		when others then
			errorNumber := SQLSTATE;
			errorMessage := SQLERRM;
			--Handle errors.
			select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
			--End Proc
			select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
			return 16;
		end;
		stepCt := stepCt + 1; 
		select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Deleted existing indexes for ' ||
				case when p_table = 'ALL' then p_schema else p_table end,rowCt,stepCt,'Done') into rtnCd;
		
		--	insert indexes for schema tables into cz.table_index
		
		begin
		insert into tm_cz.table_index
		(schema_name
		,table_name
		,index_name
		,index_sql
		)
		select distinct n.nspname as schema_name
			  ,c.relname as table_name
			  ,a.relname as index_name
			  ,pg_get_indexdef(b.indexrelid) as index_sql
		from pg_class a
		inner join pg_index b
			  on  a.oid = b.indexrelid 
		inner join pg_class c
			  on  b.indrelid = c.oid
		inner join pg_attribute d
			  on  c.oid = d.attrelid 
			  and d.attnum = any(b.indkey)
		inner join pg_namespace n
			  on n.oid = a.relnamespace
		where a.relname not like 'pg_%'
		  and n.nspname = p_schema
		  and case when p_table = 'ALL' then 1
				   else case when p_table = c.relname then 1
							 else 0 end
				   end = 1
		order by schema_name, table_name, index_name;
		get diagnostics rowCt := ROW_COUNT;
		exception 
		when others then
			errorNumber := SQLSTATE;
			errorMessage := SQLERRM;
			--Handle errors.
			select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
			--End Proc
			select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
			return 16;
		end;
		stepCt := stepCt + 1; 
		select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Inserted indexes for ' ||
				case when p_table = 'ALL' then p_schema else p_table end,rowCt,stepCt,'Done') into rtnCd;
	else 
		--	drop indexes for DROP or ADD
		
		for v_index in (select * from tm_cz.table_index 
						where schema_name = p_schema 
						  and case when p_table = 'ALL' then 1
								   else case when p_table = table_name then 1
											 else 0
											 end
								   end = 1)
		loop
			v_sql = 'drop index if exists ' || v_index.schema_name || '.' || v_index.index_name || ' cascade';
			begin
			execute v_sql;
			get diagnostics rowCt := ROW_COUNT;
			--exception 
			--when others then
			--	errorNumber := SQLSTATE;
			--	errorMessage := SQLERRM;
				--Handle errors.
			--	select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
				--End Proc
			--	select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
			--	return 16;
			end;
			stepCt := stepCt + 1;
			select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'Dropped index ' || v_index.index_name || ' for ' ||
				case when p_table = 'ALL' then p_schema else p_table end,rowCt,stepCt,'Done') into rtnCd;
			
			if p_run_type = 'ADD' then
				--	add indexes for schema
				v_sql = v_index.index_sql;
				begin
				execute v_sql;
				get diagnostics rowCt := ROW_COUNT;
				exception 
				when others then
					stepCt := stepCt + 1;
					select tm_cz.czx_write_audit(jobId,databaseName,procedureName,v_index.index_sql || ': ' || sqlerrm,0,stepCt,'Done') into rtnCd;
					errorNumber := SQLSTATE;
					errorMessage := SQLERRM;
					--Handle errors.
					select tm_cz.czx_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
					--End Proc
					select tm_cz.czx_end_audit (jobID, 'FAIL') into rtnCd;
					return 16;
				end;
				stepCt := stepCt + 1;
				select tm_cz.czx_write_audit(jobId,databaseName,procedureName, 'Added index ' || v_index.index_name || ' on ' || p_schema || '.' || v_index.table_name,0,stepCt,'Done') into rtnCd;
			end if;
		end loop;
	end if;

    ---Cleanup OVERALL JOB if this proc is being run standalone

	stepCt := stepCt + 1;
	select tm_cz.czx_write_audit(jobId,databaseName,procedureName,'End ' || procedureName,0,stepCt,'Done') into rtnCd;

	---Cleanup OVERALL JOB if this proc is being run standalone
	IF newJobFlag = 1
	THEN
		select tm_cz.czx_end_audit (jobID, 'SUCCESS') into rtnCd;
	END IF;

	return 1;
	

end
$$;

