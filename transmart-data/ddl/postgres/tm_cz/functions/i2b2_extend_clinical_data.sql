--
-- Name: i2b2_extend_clinical_data(character varying, character varying, character varying, character varying, numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_extend_clinical_data(trial_id character varying, top_node character varying, secure_study character varying DEFAULT 'N'::character varying, highlight_study character varying DEFAULT 'N'::character varying, currentjobid numeric DEFAULT (-1)) RETURNS numeric
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
-- This function is very simular to "i2b2_load_clinical_data()" except that this
-- function doesnot delete existing clinical data in the tables.
-- With this function you can "extend" the already existing clinical data for a study.
-- Note: You can not modify already existing data in de database with this function
--       It probably a good idea to combine this function with "i2b2_load_clinical_data()"
--       and do some refactoring on the way.

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
	rtnCd			integer;
	
	topNode			varchar(2000);
	topLevel		numeric(10,0);
	root_node		varchar(2000);
	root_level		integer;
	study_name		varchar(2000);
	TrialID			varchar(100);
	secureStudy		varchar(200);
	etlDate			timestamp;
	tPath			varchar(2000);
	pCount			integer;
	pExists			integer;
	rtnCode			integer;
	tText			varchar(2000);
  
	addNodes CURSOR is
	select DISTINCT leaf_node, node_name
	from  tm_wz.wt_trial_nodes a;
   
	--	cursor to define the path for delete_one_node  this will delete any nodes that are hidden after i2b2_create_concept_counts

	delNodes CURSOR is
	select distinct c_fullname 
	from  i2b2metadata.i2b2
	where c_fullname like topNode || '%' escape '`'
      and substr(c_visualattributes,2,1) = 'H';
	  
	--	cursor to determine if any leaf nodes exist in i2b2 that are not used in this reload (node changes from text to numeric or numeric to text)
	  
	delUnusedLeaf cursor is
	select l.c_fullname
	from i2b2metadata.i2b2 l
	where l.c_visualattributes like 'L%'
	  and l.c_fullname like topNode || '%' escape '`'
	  and l.c_fullname not in
		 (select t.leaf_node 
		  from tm_wz.wt_trial_nodes t
		  union
		  select m.c_fullname
		  from deapp.de_subject_sample_mapping sm
			  ,i2b2metadata.i2b2 m
		  where sm.trial_name = TrialId
		    and sm.concept_code = m.c_basecode
			and m.c_visualattributes like 'L%');
BEGIN
  
	TrialID := upper(trial_id);
	secureStudy := upper(secure_study);
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_LOAD_CLINICAL_DATA';

	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it

	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select tm_cz.cz_start_audit (procedureName, databaseName) into jobID;
	END IF;
    	
	stepCt := 0;
	stepCt := stepCt + 1;
	tText := 'Start i2b2_extend_clinical_data for ' || TrialId;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,tText,0,stepCt,'Done') into rtnCd;
	
	if (secureStudy not in ('Y','N') ) then
		secureStudy := 'Y';
	end if;
	
	topNode := REGEXP_REPLACE('\' || top_node || '\','(\\){2,}', '\', 'g');
	
	--	figure out how many nodes (folders) are at study name and above
	--	\Public Studies\Clinical Studies\Pancreatic_Cancer_Smith_GSE22780\: topLevel = 4, so there are 3 nodes
	--	\Public Studies\GSE12345\: topLevel = 3, so there are 2 nodes
	
	select length(topNode)-length(replace(topNode,'\','')) into topLevel;
	
	if topLevel < 3 then
		stepCt := stepCt + 1;
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Path specified in top_node must contain at least 2 nodes',0,stepCt,'Done') into rtnCd;	
		select tm_cz.cz_error_handler (jobID, procedureName, '-1', 'Application raised error') into rtnCd;
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;	
	end if;	
	
	--	delete any existing data from lz_src_clinical_data and load new data
	begin
	delete from tm_lz.lz_src_clinical_data
	where study_id = TrialId;
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete existing data from lz_src_clinical_data',rowCt,stepCt,'Done') into rtnCd;
	
	begin
	insert into tm_lz.lz_src_clinical_data
	(study_id
	,site_id
	,subject_id
	,visit_name
	,data_label
        ,modifier_cd
	,data_value
        ,units_cd
        ,date_timestamp
	,category_cd
	,etl_job_id
	,etl_date
	,ctrl_vocab_code)
	select study_id
		  ,site_id
		  ,subject_id
		  ,visit_name
		  ,data_label
                  ,modifier_cd
		  ,data_value
                  ,units_cd
                  ,date_timestamp
		  ,category_cd
		  ,jobId
		  ,etlDate
		  ,ctrl_vocab_code
	from tm_lz.lt_src_clinical_data;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	get diagnostics rowCt := ROW_COUNT;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Insert data into lz_src_clinical_data',rowCt,stepCt,'Done') into rtnCd;
		
	--	truncate tm_wz.wrk_clinical_data and load data from external file
	
	execute ('truncate table tm_wz.wrk_clinical_data');
	
	--	insert data from lt_src_clinical_data to tm_wz.wrk_clinical_data
	
	begin
	insert into tm_wz.wrk_clinical_data
	(study_id
	,site_id
	,subject_id
	,visit_name
	,data_label
        ,modifier_cd
	,data_value
        ,units_cd
        ,date_timestamp
	,category_cd
	,ctrl_vocab_code
	)
	select study_id
		  ,site_id
		  ,subject_id
		  ,visit_name
		  ,data_label
                  ,modifier_cd
		  ,data_value
                  ,units_cd
                  ,date_timestamp
		  ,category_cd
		  ,ctrl_vocab_code
	from tm_lz.lt_src_clinical_data;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	get diagnostics rowCt := ROW_COUNT;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Load lt_src_clinical_data to work table',rowCt,stepCt,'Done') into rtnCd;

	-- Get root_node from topNode
  
	select tm_cz.parse_nth_value(topNode, 2, '\') into root_node;
	
	select count(*) into pExists
	from i2b2metadata.table_access
	where c_name = root_node;
	
	select count(*) into pCount
	from i2b2metadata.i2b2
	where c_name = root_node;
	
	if pExists = 0 or pCount = 0 then
		select tm_cz.i2b2_add_root_node(root_node, jobId) into rtnCd;
	end if;
	
	select c_hlevel into root_level
	from i2b2metadata.table_access
	where c_name = root_node;
	
	-- Get study name from topNode
  
	select tm_cz.parse_nth_value(topNode, topLevel, '\') into study_name;
	
	--	Add any upper level nodes as needed
	
	tPath := REGEXP_REPLACE(replace(top_node,study_name,''),'(\\){2,}', '\', 'g');
	select length(tPath) - length(replace(tPath,'\','')) into pCount;

	if pCount > 2 then
		stepCt := stepCt + 1;
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Adding upper-level nodes',0,stepCt,'Done') into rtnCd;
		select tm_cz.i2b2_fill_in_tree(null, tPath, jobId) into rtnCd;
	end if;

	select count(*) into pExists
	from i2b2metadata.i2b2
	where c_fullname = topNode;
	
	--	add top node for study
	
	if pExists = 0 then
		select tm_cz.i2b2_add_node(TrialId, topNode, study_name, jobId) into rtnCd;
	end if;
  
	--	Set data_type, category_path, and usubjid 
  
	update tm_wz.wrk_clinical_data
	set data_type = 'T'
	   ,category_path = replace(replace(category_cd,'_',' '),'+','\')
	   ,usubjid = REGEXP_REPLACE(TrialID || ':' || coalesce(site_id,'') || ':' || subject_id,
                   '(::){1,}', ':', 'g'); 
	 get diagnostics rowCt := ROW_COUNT;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Set columns in tm_wz.wrk_clinical_data',rowCt,stepCt,'Done') into rtnCd;

	--	Delete rows where data_value is null
  
	begin
	delete from tm_wz.wrk_clinical_data
	where data_value is null;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	get diagnostics rowCt := ROW_COUNT;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete null data_values in tm_wz.wrk_clinical_data',rowCt,stepCt,'Done') into rtnCd;
	
	--Remove Invalid pipes in the data values.
	--RULE: If Pipe is last or first, delete it
	--If it is in the middle replace with a dash

	begin
	update tm_wz.wrk_clinical_data
	set data_value = replace(trim('|' from data_value), '|', '-')
	where data_value like '%|%';
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	get diagnostics rowCt := ROW_COUNT;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Remove pipes in data_value',rowCt,stepCt,'Done') into rtnCd;
 
	--Remove invalid Parens in the data
	--They have appeared as empty pairs or only single ones.
  
	begin
	update tm_wz.wrk_clinical_data
	set data_value = replace(data_value,'(', '')
	where data_value like '%()%'
	   or data_value like '%( )%'
	   or (data_value like '%(%' and data_value NOT like '%)%');
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Remove empty parentheses 1',rowCt,stepCt,'Done') into rtnCd;
	
	begin
	update tm_wz.wrk_clinical_data
	set data_value = replace(data_value,')', '')
	where data_value like '%()%'
	   or data_value like '%( )%'
	   or (data_value like '%)%' and data_value NOT like '%(%');
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Remove empty parentheses 2',rowCt,stepCt,'Done') into rtnCd;

	--Replace the Pipes with Commas in the data_label column
	begin
	update tm_wz.wrk_clinical_data
    set data_label = replace (data_label, '|', ',')
    where data_label like '%|%';
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Replace pipes with comma in data_label',rowCt,stepCt,'Done') into rtnCd;

	--	set visit_name to null when there's only a single visit_name for the catgory
	
	begin
	update tm_wz.wrk_clinical_data tpm
	set visit_name=null
	where (tpm.category_cd) in
		  (select x.category_cd
		   from tm_wz.wrk_clinical_data x
		   group by x.category_cd
		   having count(distinct upper(x.visit_name)) = 1);
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Set single visit_name to null',rowCt,stepCt,'Done') into rtnCd;
	
	--	set data_label to null when it duplicates the last part of the category_path
	--	Remove data_label from last part of category_path when they are the same

	begin
	update tm_wz.wrk_clinical_data tpm
	--set data_label = null
	set category_path=substr(tpm.category_path,1,tm_cz.instr(tpm.category_path,'\',-2,1)-1)
	   ,category_cd=substr(tpm.category_cd,1,tm_cz.instr(tpm.category_cd,'+',-2,1)-1)
	where (tpm.category_cd, tpm.data_label) in
		  (select distinct t.category_cd
				 ,t.data_label
		   from tm_wz.wrk_clinical_data t
		   where upper(substr(t.category_path,tm_cz.instr(t.category_path,'\',-1,1)+1,length(t.category_path)-tm_cz.instr(t.category_path,'\',-1,1))) 
			     = upper(t.data_label)
		     and t.data_label is not null)
	  and tpm.data_label is not null;
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Set data_label to null when found in category_path',rowCt,stepCt,'Done') into rtnCd;

	--	set visit_name to null if same as data_label
	
	begin
	update tm_wz.wrk_clinical_data t
	set visit_name=null
	where (t.category_cd, t.visit_name, t.data_label) in
	      (select distinct tpm.category_cd
				 ,tpm.visit_name
				 ,tpm.data_label
		  from tm_wz.wrk_clinical_data tpm
		  where tpm.visit_name = tpm.data_label);
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Set visit_name to null when found in data_label',rowCt,stepCt,'Done') into rtnCd;

	--	set visit_name to null if same as data_value
	
	begin
	update tm_wz.wrk_clinical_data t
	set visit_name=null
	where (t.category_cd, t.visit_name, t.data_value) in
	      (select distinct tpm.category_cd
				 ,tpm.visit_name
				 ,tpm.data_value
		  from tm_wz.wrk_clinical_data tpm
		  where tpm.visit_name = tpm.data_value);
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Set visit_name to null when found in data_value',rowCt,stepCt,'Done') into rtnCd;

	--	set visit_name to null if only DATALABEL in category_cd
	
	begin
	update tm_wz.wrk_clinical_data t
	set visit_name=null
	where t.category_cd like '%DATALABEL%'
	  and t.category_cd not like '%VISITNAME%';
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Set visit_name to null when only DATALABE in category_cd',rowCt,stepCt,'Done') into rtnCd;
	
	--	change any % to Pct and & and + to ' and ' and _ to space in data_label only
	
	begin
	update tm_wz.wrk_clinical_data
	set data_label=replace(replace(replace(replace(data_label,'%',' Pct'),'&',' and '),'+',' and '),'_',' ')
	   ,data_value=replace(replace(replace(data_value,'%',' Pct'),'&',' and '),'+',' and ')
	   ,category_cd=replace(replace(category_cd,'%',' Pct'),'&',' and ')
	   ,category_path=replace(replace(category_path,'%',' Pct'),'&',' and ');
	   exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;

  --Trim trailing and leadling spaces as well as remove any double spaces, remove space from before comma, remove trailing comma

	begin
	update tm_wz.wrk_clinical_data
	set data_label  = trim(trailing ',' from trim(replace(replace(data_label,'  ', ' '),' ,',','))),
		data_value  = trim(trailing ',' from trim(replace(replace(data_value,'  ', ' '),' ,',','))),
--		sample_type = trim(trailing ',' from trim(replace(replace(sample_type,'  ', ' '),' ,',','))),
		visit_name  = trim(trailing ',' from trim(replace(replace(visit_name,'  ', ' '),' ,',',')));
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Remove leading, trailing, double spaces',rowCt,stepCt,'Done') into rtnCd;

    --1. DETERMINE THE DATA_TYPES OF THE FIELDS
	--	replaced cursor with update, used temp table to store category_cd/data_label because correlated subquery ran too long
	
	execute ('truncate table tm_wz.wt_num_data_types');

	begin
	insert into tm_wz.wt_num_data_types
	(category_cd
	,data_label
	,visit_name
	)
    select category_cd,
           data_label,
           visit_name
    from tm_wz.wrk_clinical_data
    where data_value is not null
    group by category_cd
	        ,data_label
            ,visit_name
      having sum(tm_cz.is_numeric(data_value)) = 0;
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Insert numeric data into WZ wt_num_data_types',rowCt,stepCt,'Done') into rtnCd;

	--	Check if any duplicate records of key columns (site_id, subject_id, visit_name, data_label, category_cd) for numeric data
	--	exist.  Raise error if yes
	
	execute ('truncate table tm_wz.wt_clinical_data_dups');
	
	begin
	insert into tm_wz.wt_clinical_data_dups
	(site_id
	,subject_id
	,visit_name
	,data_label
	,category_cd
        ,modifier_cd)
	select w.site_id, w.subject_id, w.visit_name, w.data_label, w.category_cd, w.modifier_cd
	from tm_wz.wrk_clinical_data w
	where exists
		 (select 1 from tm_wz.wt_num_data_types t
		 where coalesce(w.category_cd,'@') = coalesce(t.category_cd,'@')
		   and coalesce(w.data_label,'@') = coalesce(t.data_label,'@')
		   and coalesce(w.visit_name,'@') = coalesce(t.visit_name,'@')
		  )
	group by w.site_id, w.subject_id, w.visit_name, w.data_label, w.category_cd, w.modifier_cd
	having count(*) > 1;
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Check for duplicate key columns',rowCt,stepCt,'Done') into rtnCd;
			  
	if rowCt > 0 then
		stepCt := stepCt + 1;
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Duplicate values found in key columns',0,stepCt,'Done') into rtnCd;	
		select tm_cz.cz_error_handler (jobID, procedureName, '-1', 'Application raised error') into rtnCd;
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;	
	end if;
	
	--	check for multiple visit_names for category_cd, data_label, data_value
	
     select max(case when x.null_ct > 0 and x.non_null_ct > 0
					 then 1 else 0 end) into pCount
      from (select category_cd, data_label, data_value
				  ,sum(case when visit_name is null then 1 else 0 end) as null_ct
				  ,sum(case when visit_name is null then 0 else 1 end) as non_null_ct
			from tm_lz.lt_src_clinical_data
			where (category_cd like '%VISITNAME%' or
				   category_cd not like '%DATALABEL%')
			group by category_cd, data_label, data_value) x;
	get diagnostics rowCt := ROW_COUNT;  
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Check for multiple visit_names for category/label/value ',rowCt,stepCt,'Done') into rtnCd;
			  
	if pCount > 0 then
		stepCt := stepCt + 1;
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Multiple visit names for category/label/value',0,stepCt,'Done') into rtnCd;	
		select tm_cz.cz_error_handler (jobID, procedureName, '-1', 'Application raised error') into rtnCd;
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;	
	end if;
		
	begin
	update tm_wz.wrk_clinical_data t
	set data_type='N'
	where exists
	     (select 1 from tm_wz.wt_num_data_types x
	      where coalesce(t.category_cd,'@') = coalesce(x.category_cd,'@')
			and coalesce(t.data_label,'**NULL**') = coalesce(x.data_label,'**NULL**')
			and coalesce(t.visit_name,'**NULL**') = coalesce(x.visit_name,'**NULL**')
		  );
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Updated data_type flag for numeric data_types',rowCt,stepCt,'Done') into rtnCd;

	-- Build all needed leaf nodes in one pass for both numeric and text nodes
 
	execute ('truncate table tm_wz.wt_trial_nodes');
	
	begin
	insert into tm_wz.wt_trial_nodes
	(leaf_node
	,category_cd
	,visit_name
	,data_label
	,data_value
	,data_type
	)
    select DISTINCT 
    Case 
	--	Text data_type (default node)
	When a.data_type = 'T'
	     then case when a.category_path like '%DATALABEL%' and a.category_path like '%VISITNAME%'
		      then regexp_replace(topNode || replace(replace(coalesce(a.category_path,''),'DATALABEL',coalesce(a.data_label,'')),'VISITNAME',coalesce(a.visit_name,'')) || '\' || coalesce(a.data_value,'') || '\','(\\){2,}', '\', 'g')
			  when a.category_path like '%DATALABEL%'
			  then regexp_replace(topNode || replace(coalesce(a.category_path,''),'DATALABEL',coalesce(a.data_label,'')) || '\' || coalesce(a.data_value,'') || '\','(\\){2,}', '\', 'g')
			  else REGEXP_REPLACE(topNode || coalesce(a.category_path,'') || 
                   '\'  || coalesce(a.data_label,'') || '\' || coalesce(a.data_value,'') || '\' || coalesce(a.visit_name,'') || '\',
                   '(\\){2,}', '\', 'g') 
			  end
	--	else is numeric data_type and default_node
	else case when a.category_path like '%DATALABEL%' and a.category_path like '%VISITNAME%'
		      then regexp_replace(topNode || replace(replace(coalesce(a.category_path,''),'DATALABEL',coalesce(a.data_label,'')),'VISITNAME',coalesce(a.visit_name,'')) || '\','(\\){2,}', '\', 'g')
			  when a.category_path like '%DATALABEL%'
			  then regexp_replace(topNode || replace(coalesce(a.category_path,''),'DATALABEL',coalesce(a.data_label,'')) || '\','(\\){2,}', '\', 'g')
			  else REGEXP_REPLACE(topNode || coalesce(a.category_path,'') || 
                   '\'  || coalesce(a.data_label,'') || '\' || coalesce(a.visit_name,'') || '\',
                   '(\\){2,}', '\', 'g')
			  end
	end as leaf_node,
    a.category_cd,
    a.visit_name,
	a.data_label,
	case when a.data_type = 'T' then a.data_value else null end as data_value
    ,a.data_type
	from  tm_wz.wrk_clinical_data a;
	get diagnostics rowCt := ROW_COUNT; 
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Create leaf nodes for trial',rowCt,stepCt,'Done') into rtnCd;

	--	set node_name
	
	begin
	update tm_wz.wt_trial_nodes
	set node_name=tm_cz.parse_nth_value(leaf_node,length(leaf_node)-length(replace(leaf_node,'\','')),'\');
	get diagnostics rowCt := ROW_COUNT; 
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Updated node name for leaf nodes',rowCt,stepCt,'Done') into rtnCd;

	--	insert subjects into patient_dimension if needed
	
	execute ('truncate table tm_wz.wt_subject_info');

	begin
	insert into tm_wz.wt_subject_info
	(usubjid,
     age_in_years_num,
     sex_cd,
     race_cd
    )
	select a.usubjid,
	      coalesce(max(case when upper(a.data_label) = 'AGE'
					   then case when tm_cz.is_numeric(a.data_value) = 1 then 0 else a.data_value::integer end
		               when upper(a.data_label) like '%(AGE)' 
					   then case when tm_cz.is_numeric(a.data_value) = 1 then 0 else a.data_value::integer end
					   else null end),0) as age,
		  coalesce(max(case when upper(a.data_label) = 'SEX' then a.data_value
		           when upper(a.data_label) like '%(SEX)' then a.data_value
				   when upper(a.data_label) = 'GENDER' then a.data_value
				   else null end),'Unknown') as sex,
		  max(case when upper(a.data_label) = 'RACE' then a.data_value
		           when upper(a.data_label) like '%(RACE)' then a.data_value
				   else null end) as race
	from tm_wz.wrk_clinical_data a
	group by a.usubjid;
	get diagnostics rowCt := ROW_COUNT;	
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Insert subject information into temp table',rowCt,stepCt,'Done') into rtnCd;


        --  check if there are observation in the upload which already exist in database
        --         (is there already an observation for given patient and concept)


with maps as (select td.leaf_node, usubjid from tm_wz.wrk_clinical_data cd, tm_wz.wt_trial_nodes td where cd.data_label = td.data_label)
select count(*) into pExists from i2b2demodata.observation_fact of where (of.patient_num, of.concept_cd) IN
                           (select pd.patient_num, cd.concept_cd from i2b2demodata.patient_dimension pd 
                                                                      INNER JOIN maps on pd.sourcesystem_cd = maps.usubjid
                                                                      INNER JOIN i2b2demodata.concept_dimension cd on cd.concept_path = maps.leaf_node);

        if pExists > 0 then
                  stepCt := stepCt + 1;
                  select tm_cz.cz_write_audit(jobId,databaseName, procedureName,
				'You cannot overwrite existing observations when adding.', pExists, stepCt, 'Done') into rtnCd;    
                  select tm_cz.cz_error_handler (jobID, procedureName, '-1', 'Application raised error') into rtnCd;
                  select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
                  return -16;     
        end if; 

	--  check if data to be added is of the same data-type (numeric/text) as possible already existing
        --  observations of the concept
	
	begin
		with exist_cc as (select distinct concept_cd, valtype_cd from i2b2demodata.observation_fact of
                  where of.concept_cd IN
                     (select cd.concept_cd from i2b2demodata.concept_dimension cd, tm_wz.wt_trial_nodes td
			where cd.concept_path = td.leaf_node))
                select count(*) into pCount from exist_cc, i2b2demodata.observation_fact of 
			where exist_cc.concept_cd = of.concept_cd 
				and exist_cc.valtype_cd != of.valtype_cd;
		if pCount > 0 then
                  stepCt := stepCt + 1;
                  select tm_cz.cz_write_audit(jobId,databaseName, procedureName,
				'You cannot upload different datatype for concept then already in database.', pCount, stepCt, 'Done') into rtnCd;    
                  select tm_cz.cz_error_handler (jobID, procedureName, '-1', 'Application raised error') into rtnCd;
                  select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
                  return -16;     
 		end if;
	end;


	--	insert new subjects into patient_dimension
	
	begin
	insert into i2b2demodata.patient_dimension
    (patient_num,
     sex_cd,
     age_in_years_num,
     race_cd,
     update_date,
     download_date,
     import_date,
     sourcesystem_cd
    )
    select nextval('i2b2demodata.seq_patient_num'),
		   t.sex_cd,
		   t.age_in_years_num,
		   t.race_cd,
		   current_timestamp,
		   current_timestamp,
		   current_timestamp,
		   t.usubjid
    from tm_wz.wt_subject_info t
	where t.usubjid in 
		 (select distinct cd.usubjid from tm_wz.wt_subject_info cd
		  except
		  select distinct pd.sourcesystem_cd from i2b2demodata.patient_dimension pd
		  where pd.sourcesystem_cd like TrialId || ':%');
	get diagnostics rowCt := ROW_COUNT;	
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Insert new subjects into patient_dimension',rowCt,stepCt,'Done') into rtnCd;
		
	
	--	bulk insert leaf nodes
	begin
	with ncd as (select t.leaf_node, t.node_name from tm_wz.wt_trial_nodes t)
	update i2b2demodata.concept_dimension
	set name_char=ncd.node_name
	from ncd
	where concept_path = ncd.leaf_node;
	get diagnostics rowCt := ROW_COUNT;	
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;	
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Update name_char in concept_dimension for changed names',rowCt,stepCt,'Done') into rtnCd;
	
	begin
	insert into i2b2demodata.concept_dimension
    (concept_cd
	,concept_path
	,name_char
	,update_date
	,download_date
	,import_date
	,sourcesystem_cd
	)
    select nextval('i2b2demodata.concept_id')
	     ,x.leaf_node
		 ,x.node_name
		 ,current_timestamp
		 ,current_timestamp
		 ,current_timestamp
		 ,TrialId
	from (select distinct c.leaf_node
				,c.node_name::text as node_name
		  from tm_wz.wt_trial_nodes c
		  where not exists
			(select 1 from i2b2demodata.concept_dimension x
			where c.leaf_node = x.concept_path)
		 ) x;
	get diagnostics rowCt := ROW_COUNT;	
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Inserted new leaf nodes into I2B2DEMODATA concept_dimension',rowCt,stepCt,'Done') into rtnCd;
	
			   
	begin
	insert into i2b2metadata.i2b2
    (c_hlevel
	,c_fullname
	,c_name
	,c_visualattributes
	,c_synonym_cd
	,c_facttablecolumn
	,c_tablename
	,c_columnname
	,c_dimcode
	,c_tooltip
	,update_date
	,download_date
	,import_date
	,sourcesystem_cd
	,c_basecode
	,c_operator
	,c_columndatatype
	,c_comment
	,m_applied_path
	,c_metadataxml
	)
    select (length(c.concept_path) - coalesce(length(replace(c.concept_path, '\','')),0)) / length('\') - 2 + root_level
		  ,c.concept_path
		  ,c.name_char
		  ,'LA'
		  ,'N'
		  ,'CONCEPT_CD'
		  ,'CONCEPT_DIMENSION'
		  ,'CONCEPT_PATH'
		  ,c.concept_path
		  ,c.concept_path
		  ,current_timestamp
		  ,current_timestamp
		  ,current_timestamp
		  ,c.sourcesystem_cd
		  ,c.concept_cd
		  ,'LIKE'
		  ,'T'
		  ,'trial:' || TrialID 
		  ,'@'
		  ,case when t.data_type = 'T' then null
		   else '<?xml version="1.0"?><ValueMetadata><Version>3.02</Version><CreationDateTime>08/14/2008 01:22:59</CreationDateTime><TestID></TestID><TestName></TestName><DataType>PosFloat</DataType><CodeType></CodeType><Loinc></Loinc><Flagstouse></Flagstouse><Oktousevalues>Y</Oktousevalues><MaxStringLength></MaxStringLength><LowofLowValue>0</LowofLowValue><HighofLowValue>0</HighofLowValue><LowofHighValue>100</LowofHighValue>100<HighofHighValue>100</HighofHighValue><LowofToxicValue></LowofToxicValue><HighofToxicValue></HighofToxicValue><EnumValues></EnumValues><CommentsDeterminingExclusion><Com></Com></CommentsDeterminingExclusion><UnitValues><NormalUnits>ratio</NormalUnits><EqualUnits></EqualUnits><ExcludingUnits></ExcludingUnits><ConvertingUnits><Units></Units><MultiplyingFactor></MultiplyingFactor></ConvertingUnits></UnitValues><Analysis><Enums /><Counts /><New /></Analysis></ValueMetadata>'
		   end
    from i2b2demodata.concept_dimension c
		,tm_wz.wt_trial_nodes t
    where c.concept_path = t.leaf_node
	  and not exists
		 (select 1 from i2b2metadata.i2b2 x
		  where c.concept_path = x.c_fullname);
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Inserted leaf nodes into I2B2METADATA i2b2',rowCt,stepCt,'Done') into rtnCd;

	
    --Insert into observation_fact
	
	begin
	insert into i2b2demodata.observation_fact
	(encounter_num,
     patient_num,
     concept_cd,
     start_date,
     modifier_cd,
     valtype_cd,
     tval_char,
     nval_num,
     units_cd,
     sourcesystem_cd,
     import_date,
     valueflag_cd,
     provider_id,
     location_cd,
     instance_num
	)
	select distinct c.patient_num,
		   c.patient_num,
		   i.c_basecode,
		   coalesce(a.date_timestamp, 'infinity'),
		   coalesce(a.modifier_cd, '@'),
		   a.data_type,
		   case when a.data_type = 'T' then a.data_value
				else 'E'  --Stands for Equals for numeric types
				end,
		   case when a.data_type = 'N' then a.data_value::numeric
				else null --Null for text types
				end,
                   a.units_cd,
		   a.study_id, 
		   current_timestamp, 
		   '@',
		   '@',
		   '@',
                   1
	from tm_wz.wrk_clinical_data a
		,i2b2demodata.patient_dimension c
		,tm_wz.wt_trial_nodes t
		,i2b2metadata.i2b2 i
	where a.usubjid = c.sourcesystem_cd
	  and coalesce(a.category_cd,'@') = coalesce(t.category_cd,'@')
	  and coalesce(a.data_label,'**NULL**') = coalesce(t.data_label,'**NULL**')
	  and coalesce(a.visit_name,'**NULL**') = coalesce(t.visit_name,'**NULL**')
	  and case when a.data_type = 'T' then a.data_value else '**NULL**' end = coalesce(t.data_value,'**NULL**')
	  and t.leaf_node = i.c_fullname
	  and not exists		-- don't insert if lower level node exists
		 (select 1 from tm_wz.wt_trial_nodes x
		  where x.leaf_node like t.leaf_node || '%_' escape '`')
	  and a.data_value is not null;
	get diagnostics rowCt := ROW_COUNT; 
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Insert trial into I2B2DEMODATA observation_fact',rowCt,stepCt,'Done') into rtnCd;


	-- final procs
  
	select tm_cz.i2b2_fill_in_tree(TrialId, topNode, jobID) into rtnCd;
	
	--	set sourcesystem_cd, c_comment to null if any added upper-level nodes
		

	select tm_cz.i2b2_create_concept_counts(topNode, jobID) into rtnCd;
	
	--	delete each node that is hidden after create concept counts
	
	 FOR r_delNodes in delNodes Loop

    --	deletes hidden nodes for a trial one at a time

		select tm_cz.i2b2_delete_1_node(r_delNodes.c_fullname) into rtnCd;
		stepCt := stepCt + 1;
		tText := 'Deleted node: ' || r_delNodes.c_fullname;
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,tText,rowCt,stepCt,'Done') into rtnCd;

	END LOOP;  	

	select tm_cz.i2b2_create_security_for_trial(TrialId, secureStudy, jobID) into rtnCd;
	select tm_cz.i2b2_load_security_data(jobID) into rtnCd;
	
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'End i2b2_extend_clinical_data',0,stepCt,'Done') into rtnCd;
	
	---Cleanup OVERALL JOB if this proc is being run standalone
	IF newJobFlag = 1
	THEN
		select tm_cz.cz_end_audit (jobID, 'SUCCESS') into rtnCd;
	END IF;

	return 1;
/*	
	EXCEPTION
	WHEN OTHERS THEN
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
*/
END;

$$;

