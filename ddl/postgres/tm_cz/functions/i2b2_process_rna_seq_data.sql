--
-- Name: i2b2_process_rna_seq_data(character varying, character varying, character varying, character varying, numeric, character varying, numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_process_rna_seq_data(trial_id character varying, top_node character varying, data_type character varying DEFAULT 'R'::character varying, source_code character varying DEFAULT 'STD'::character varying, log_base numeric DEFAULT 2, secure_study character varying DEFAULT NULL::character varying, currentjobid numeric DEFAULT (-1)) RETURNS numeric
    LANGUAGE plpgsql
    AS $$
DECLARE

/*************************************************************************
* This stored procedure is for ETL to load RNA sequencing
* Date:10/23/2013
******************************************************************/
--	***  NOTE ***
--	The input file columns are mapped to the following table columns.  This is done so that the javascript for the advanced workflows
--	selects the correct data for the dropdowns.

--		tissue_type	=>	sample_type
--		attribute_1	=>	tissue_type
--		atrribute_2	=>	timepoint	

  TrialID		varchar(100);
  RootNode		varchar(2000);
  root_level	integer;
  topNode		varchar(2000);
  topLevel		integer;
  tPath			varchar(2000);
  study_name	varchar(100);
  sourceCd		varchar(50);
  secureStudy	varchar(1);

  dataType		varchar(10);
  sqlText		varchar(1000);
  tText			varchar(1000);
  gplTitle		varchar(1000);
  pExists		bigint;
  partTbl   	bigint;
  partExists 	bigint;
  sampleCt		bigint;
  idxExists 	bigint;
  logBase		bigint;
  pCount		integer;
  sCount		integer;
  tablespaceName	varchar(200);
  v_bio_experiment_id	bigint;
   partitioniD	numeric(18,0);
  partitionName	varchar(100);
  partitionIndx	varchar(100);
  
    --Audit variables
  newJobFlag integer;
  databaseName varchar(100);
  procedureName varchar(100);
  jobID bigint;
  stepCt bigint;
  rowCt			numeric(18,0);
  errorNumber		character varying;
  errorMessage	character varying;
  rtnCd			integer;
 
	addNodes CURSOR FOR
	SELECT distinct t.leaf_node
          ,t.node_name
	from  tm_wz.wt_RNA_SEQ_nodes t
	where not exists
		 (select 1 from i2b2metadata.i2b2 x
		  where t.leaf_node = x.c_fullname);

 
--	cursor to define the path for delete_one_node  this will delete any nodes that are hidden after i2b2_create_concept_counts

  delNodes CURSOR FOR
  SELECT distinct c_fullname 
  from  i2b2metadata.i2b2
  where c_fullname like topNode || '%'
    and substring(c_visualattributes from 2 for 1) = 'H';
    --and c_visualattributes like '_H_';

	uploadI2b2 CURSOR FOR
    select category_cd,display_value,display_label,display_unit from
    tm_lz.lt_src_rna_display_mapping;

BEGIN
	TrialID := upper(trial_id);
	secureStudy := upper(secure_study);
	
	if (secureStudy not in ('Y','N') ) then
		secureStudy := 'Y';
	end if;
	
	topNode := REGEXP_REPLACE('\' || top_node || '\','(\\){2,}', '\','g');
	select length(topNode)-length(replace(topNode,'\','')) into topLevel ;
	
	if coalesce(data_type::text, '') = '' then
		dataType := 'R';
	else
		if data_type in ('R','T','L') then
			dataType := data_type;
		else
			dataType := 'R';
		end if;
	end if;
	
	logBase := log_base;
	sourceCd := upper(coalesce(source_code,'STD'));

  --Set Audit Parameters
  newJobFlag := 0; -- False (Default)
  jobID := currentJobID;

    databaseName := 'TM_CZ';
	procedureName := 'I2B2_PROCESS_RNA_SEQ_DATA';

  --Audit JOB Initialization
  --If Job ID does not exist, then this is a single procedure run and we need to create it
  IF(coalesce(jobID::text, '') = '' or jobID < 1)
  THEN
    newJobFlag := 1; -- True
    select cz_start_audit (procedureName, databaseName, jobID) into jobId;
  END IF;
    	
	stepCt := 0;
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_process_RNA_SEQ_data',0,stepCt,'Done') into rtnCd;
	
	--	Get count of records in lt_src_RNA_SEQ_subj_samp_map
	
	select count(*) into sCount
	from tm_lz.lt_src_RNA_SEQ_subj_samp_map;
	
	--	check if all subject_sample map records have a platform, If not, abort run
	
	select count(*) into pCount
	from tm_lz.lt_src_RNA_SEQ_subj_samp_map
	where coalesce(platform::text, '') = '';
	
	if pCount > 0 then
		select cz_write_audit(jobId,databasename,procedurename,'Platform data missing from one or more subject_sample mapping records',1,stepCt,'ERROR') into rtnCd;
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		select cz_end_audit (jobId,'FAIL') into rtnCd;
		return 161;
	end if;
  	
	--	check if all subject_sample map records have a tissue_type, If not, abort run
	
	select count(*) into pCount
	from tm_lz.lt_src_RNA_SEQ_subj_samp_map
	where coalesce(tissue_type::text, '') = '';
	
	if pCount > 0 then
		select cz_write_audit(jobId,databasename,procedurename,'Tissue Type data missing from one or more subject_sample mapping records',1,stepCt,'ERROR') into rtnCd;
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		select CZ_END_AUDIT (JOBID,'FAIL') into rtnCd;
		return 162;
	end if;
	
	--	check if there are multiple platforms, if yes, then platform must be supplied in lt_src_RNA_SEQ_data
	
	select count(*) into pCount
	from (select sample_cd
		  from tm_lz.lt_src_RNA_SEQ_subj_samp_map
		  group by sample_cd
		  having count(distinct platform) > 1) as x;
	
	if pCount > 0 then
		select cz_write_audit(jobId,databasename,procedurename,'Multiple platforms for sample_cd in lt_src_RNA_SEQ_subj_samp_map',1,stepCt,'ERROR') into rtnCd;
		select CZ_ERROR_HANDLER(JOBID,PROCEDURENAME) into rtnCd;
		select cz_end_audit (jobId,'FAIL') into rtnCd;
		return 164;
	end if;
		
	-- Get root_node from topNode
  
	select parse_nth_value(topNode, 2, '\') into RootNode ;
	
	select count(*) into pExists
	from i2b2metadata.table_access
	where c_name = rootNode;
	
	if pExists = 0 then
		perform i2b2_add_root_node(rootNode, jobId);
	end if;
	
	select c_hlevel into root_level
	from i2b2metadata.i2b2
	where c_name = RootNode;
	
	-- Get study name from topNode
  
	select parse_nth_value(topNode, topLevel, '\') into study_name ;
	
	--	Add any upper level nodes as needed
	
	tPath := REGEXP_REPLACE(replace(top_node,study_name,''),'(\\){2,}', '\', 'g');
	select length(tPath) - length(replace(tPath,'\','')) into pCount ;

	if pCount > 2 then
		perform i2b2_fill_in_tree(null, tPath, jobId);
	end if;

	--	uppercase study_id in lt_src_RNA_SEQ_subj_samp_map in case curator forgot
	begin
	update tm_lz.lt_src_RNA_SEQ_subj_samp_map
	set trial_name=upper(trial_name);
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		-- select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end;

	select count(*) into partExists
	from deapp.de_subject_sample_mapping sm
	where sm.trial_name = TrialId
	and coalesce(sm.source_cd,'STD') = sourceCd
	and sm.platform = 'RNA_AFFYMETRIX'
	and sm.partition_id is not null;
	
	if partExists = 0 then
		select nextval('deapp.seq_rna_partition_id') into partitionId;
	else
		select distinct partition_id into partitionId
		from deapp.de_subject_sample_mapping sm
		where sm.trial_name = TrialId
		and coalesce(sm.source_cd,'STD') = sourceCd
		and sm.platform = 'RNA_AFFYMETRIX';
	end if;

	partitionName := 'deapp.de_subject_rna_data_' || partitionId::text;
	partitionIndx := 'de_subject_rna_data_' || partitionId::text;	
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Uppercase trial_name in lt_src_RNA_SEQ_subj_samp_map',rowCt,stepCt,'Done') into rtnCd;	
	
	--	create records in patient_dimension for subject_ids if they do not exist
	--	format of sourcesystem_cd:  trial:[site:]subject_cd
	
	begin
	insert into i2b2demodata.patient_dimension
    ( patient_num,
      sex_cd,
      age_in_years_num,
      race_cd,
      update_date,
      download_date,
      import_date,
      sourcesystem_cd
    )
    select nextval('i2b2demodata.seq_patient_num')
		  ,x.sex_cd
		  ,x.age_in_years_num
		  ,x.race_cd
		  ,LOCALTIMESTAMP
		  ,LOCALTIMESTAMP
		  ,LOCALTIMESTAMP
		  ,x.sourcesystem_cd
	from (select distinct 'Unknown' as sex_cd,
				 0 as age_in_years_num,
				 null as race_cd,
				 regexp_replace(TrialId || ':' || coalesce(s.site_id,'') || ':' || s.subject_id,'(::){1,}', ':', 'g') as sourcesystem_cd
		 from tm_lz.lt_src_RNA_SEQ_subj_samp_map s
		 where (s.subject_id IS NOT NULL AND s.subject_id::text <> '')
		   and s.trial_name = TrialID
		   and s.source_cd = sourceCD
		   and not exists
			  (select 1 from i2b2demodata.patient_dimension x
			   where x.sourcesystem_cd = 
				 regexp_replace(TrialID || ':' || coalesce(s.site_id, '') || ':' || s.subject_id,'(::){1,}', ':'))
		) as x;
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
	select cz_write_audit(jobId,databaseName,procedureName,'Insert subjects to patient_dimension',rowCt,stepCt,'Done') into rtnCd;
	
	perform i2b2_create_security_for_trial(TrialId, secureStudy, jobID);

	--	Delete existing observation_fact data, will be repopulated
	begin
	delete from i2b2demodata.observation_fact obf
	where obf.concept_cd in
		 (select distinct x.concept_code
		  from deapp.de_subject_sample_mapping x
		  where x.trial_name = TrialId
		    and coalesce(x.source_cd,'STD') = sourceCD
		    and x.platform = 'RNA_AFFYMETRIX');
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
	select cz_write_audit(jobId,databaseName,procedureName,'Delete data from observation_fact',rowCt,stepCt,'Done') into rtnCd;
		
	--	Cleanup any existing data in de_subject_sample_mapping.  

	begin
	delete from deapp.DE_SUBJECT_SAMPLE_MAPPING 
	where trial_name = TrialID 
	  and coalesce(source_cd,'STD') = sourceCd
	  and platform = 'RNA_AFFYMETRIX'; --Making sure only RNA_sequencing data is deleted
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
	select cz_write_audit(jobId,databaseName,procedureName,'Delete trial from DEAPP de_subject_sample_mapping',rowCt,stepCt,'Done') into rtnCd;

--	truncate tmp node table

	EXECUTE('truncate table tm_wz.wt_RNA_SEQ_nodes');
	
--	load temp table with leaf node path, use temp table with distinct sample_type, ATTR2, platform, and title   this was faster than doing subselect
--	from wt_subject_RNA_sequencing_data

	EXECUTE('truncate table tm_wz.wt_RNA_SEQ_node_values');

	begin
	insert into tm_wz.wt_RNA_SEQ_node_values
	(category_cd
	,platform
	,tissue_type
	,attribute_1
	,attribute_2
	,title
	)
	select distinct a.category_cd
				   ,coalesce(a.platform,'GPL570')
				   ,coalesce(a.tissue_type,'Unspecified Tissue Type')
	               ,a.attribute_1
				   ,a.attribute_2
				   ,''--g.title
    from tm_lz.lt_src_RNA_SEQ_subj_samp_map a
	where a.trial_name = TrialID
	  and a.source_cd = sourceCD;
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
	select cz_write_audit(jobId,databaseName,procedureName,'Insert node values into DEAPP wt_RNA_SEQ_node_values',rowCt,stepCt,'Done') into rtnCd;
	
	begin
	insert into tm_wz.wt_RNA_SEQ_nodes
	(leaf_node
	,category_cd
	,platform
	,tissue_type
	,attribute_1
    ,attribute_2
	,node_type
	)
	select distinct topNode || regexp_replace(replace(replace(replace(replace(replace(replace(
	category_cd,'PLATFORM',title),'ATTR1',coalesce(attribute_1,'')),'ATTR2',coalesce(attribute_2,'')),'TISSUETYPE',coalesce(tissue_type,'')),'+','\'),'_',' ') || '\','(\\){2,}', '\', 'g')
		  ,category_cd
		  ,platform as platform
		  ,tissue_type
		  ,attribute_1 as attribute_1
          ,attribute_2 as attribute_2
		  ,'LEAF'
	from  tm_wz.wt_RNA_SEQ_node_values;
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
	select cz_write_audit(jobId,databaseName,procedureName,'Create leaf nodes in DEAPP tmp_RNA_SEQ_nodes',rowCt,stepCt,'Done') into rtnCd;
	
	--	insert for platform node so platform concept can be populated
		begin
	insert into tm_wz.wt_RNA_SEQ_nodes
	(leaf_node
	,category_cd
	,platform
	,tissue_type
	,attribute_1
    ,attribute_2
	,node_type
	)
	select distinct topNode || regexp_replace(replace(replace(replace(replace(replace(replace(
	substr(category_cd,1,tm_cz.instr(category_cd,'PLATFORM')+8),'PLATFORM',title),'ATTR1',coalesce(attribute_1,'')),'ATTR2',coalesce(attribute_2,'')),'TISSUETYPE',coalesce(tissue_type,'')),'+','\'),'_',' ') || '\',
	'(\\){2,}', '\', 'g')
		  ,substr(category_cd,1,instr(category_cd,'PLATFORM')+8)
		  ,platform as platform
		  ,case when instr(substr(category_cd,1,instr(category_cd,'PLATFORM')+8),'TISSUETYPE') > 1 then tissue_type else null end as tissue_type
		  ,case when instr(substr(category_cd,1,instr(category_cd,'PLATFORM')+8),'ATTR1') > 1 then attribute_1 else null end as attribute_1
          ,case when instr(substr(category_cd,1,instr(category_cd,'PLATFORM')+8),'ATTR2') > 1 then attribute_2 else null end as attribute_2
		  ,'PLATFORM'
	from  tm_wz.wt_RNA_SEQ_node_values;
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
	select cz_write_audit(jobId,databaseName,procedureName,'Create platform nodes in wt_RNA_SEQ_nodes',rowCt,stepCt,'Done') into rtnCd;
	
	--	insert for ATTR1 node so ATTR1 concept can be populated in tissue_type_cd
	begin
	insert into tm_wz.wt_RNA_SEQ_nodes
	(leaf_node
	,category_cd
	,platform
	,tissue_type
    ,attribute_1
	,attribute_2
	,node_type
	)
	select distinct topNode || regexp_replace(replace(replace(replace(replace(replace(replace(
	substr(category_cd,1,tm_cz.instr(category_cd,'ATTR1')+5),'PLATFORM',title),'ATTR1',coalesce(attribute_1,'')),'ATTR2',coalesce(attribute_2,'')),'TISSUETYPE',coalesce(tissue_type,'')),'+','\'),'_',' ') || '\',
	'(\\){2,}', '\', 'g')
		  ,substr(category_cd,1,instr(category_cd,'ATTR1')+5)
		  ,case when instr(substr(category_cd,1,instr(category_cd,'ATTR1')+5),'PLATFORM') > 1 then platform else null end as platform
		  ,case when instr(substr(category_cd,1,instr(category_cd,'ATTR1')+5),'TISSUETYPE') > 1 then tissue_type else null end as tissue_type
		  ,attribute_1 as attribute_1
          ,case when instr(substr(category_cd,1,instr(category_cd,'ATTR1')+5),'ATTR2') > 1 then attribute_2 else null end as attribute_2
		  ,'ATTR1'
	from  tm_wz.wt_RNA_SEQ_node_values
	where category_cd like '%ATTR1%'
	  and (attribute_1 IS NOT NULL AND attribute_1::text <> '');
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
	select cz_write_audit(jobId,databaseName,procedureName,'Create ATTR1 nodes in wt_RNA_SEQ_nodes',rowCt,stepCt,'Done') into rtnCd;
	
	--	insert for ATTR2 node so ATTR2 concept can be populated in timepoint_cd
	begin
	insert into tm_wz.wt_RNA_SEQ_nodes
	(leaf_node
	,category_cd
	,platform
	,tissue_type
    ,attribute_1
	,attribute_2
	,node_type
	)
	select distinct topNode || regexp_replace(replace(replace(replace(replace(replace(replace(
		substr(category_cd,1,tm_cz.instr(category_cd,'ATTR2')+5),'PLATFORM',title),'ATTR1',coalesce(attribute_1,'')),'ATTR2',coalesce(attribute_2,'')),'TISSUETYPE',coalesce(tissue_type,'')),'+','\'),'_',' ') || '\',
		'(\\){2,}', '\', 'g')
		  ,substr(category_cd,1,instr(category_cd,'ATTR2')+5)
		  ,case when instr(substr(category_cd,1,instr(category_cd,'ATTR2')+5),'PLATFORM') > 1 then platform else null end as platform
		  ,case when instr(substr(category_cd,1,instr(category_cd,'ATTR1')+5),'TISSUETYPE') > 1 then tissue_type else null end as tissue_type
          ,case when instr(substr(category_cd,1,instr(category_cd,'ATTR2')+5),'ATTR1') > 1 then attribute_1 else null end as attribute_1
		  ,attribute_2 as attribute_2
		  ,'ATTR2'
	from  tm_wz.wt_RNA_SEQ_node_values
	where category_cd like '%ATTR2%'
	  and (attribute_2 IS NOT NULL AND attribute_2::text <> '');
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
	select cz_write_audit(jobId,databaseName,procedureName,'Create ATTR2 nodes in wt_RNA_SEQ_nodes',rowCt,stepCt,'Done') into rtnCd;
	
	--	insert for tissue_type node so sample_type_cd can be populated 
	begin
	insert into tm_wz.wt_RNA_SEQ_nodes
	(leaf_node
	,category_cd
	,platform
	,tissue_type
	,attribute_1
    ,attribute_2
	,node_type
	)
	select distinct topNode || regexp_replace(replace(replace(replace(replace(replace(replace(
	substr(category_cd,1,tm_cz.instr(category_cd,'TISSUETYPE')+10),'PLATFORM',title),'ATTR1',coalesce(attribute_1,'')),'ATTR2',coalesce(attribute_2,'')),'TISSUETYPE',coalesce(tissue_type,'')),'+','\'),'_',' ') || '\',
	'(\\){2,}', '\', 'g')
		  ,substr(category_cd,1,instr(category_cd,'TISSUETYPE')+10)
		  ,case when instr(substr(category_cd,1,instr(category_cd,'TISSUETYPE')+10),'PLATFORM') > 1 then platform else null end as platform
		  ,tissue_type as tissue_type
		  ,case when instr(substr(category_cd,1,instr(category_cd,'TISSUETYPE')+10),'ATTR1') > 1 then attribute_1 else null end as attribute_1
          ,case when instr(substr(category_cd,1,instr(category_cd,'TISSUETYPE')+10),'ATTR2') > 1 then attribute_2 else null end as attribute_2
		  ,'TISSUETYPE'
	from  tm_wz.wt_RNA_SEQ_node_values
	where category_cd like '%TISSUETYPE%';
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
	select cz_write_audit(jobId,databaseName,procedureName,'Create ATTR2 nodes in wt_RNA_SEQ_nodes',rowCt,stepCt,'Done') into rtnCd;
				
	begin
	update tm_wz.wt_RNA_SEQ_nodes
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
	select cz_write_audit(jobId,databaseName,procedureName,'Updated node_name in DEAPP tmp_RNA_SEQ_nodes',rowCt,stepCt,'Done') into rtnCd;
		
--	add leaf nodes for RNA_sequencing data  The cursor will only add nodes that do not already exist.

	 FOR r_addNodes in addNodes Loop

    --Add nodes for all types (ALSO DELETES EXISTING NODE)
		begin
		perform i2b2_add_node(TrialID, r_addNodes.leaf_node, r_addNodes.node_name, jobId);
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
		tText := 'Added Leaf Node: ' || r_addNodes.leaf_node || '  Name: ' || r_addNodes.node_name;
		
		select cz_write_audit(jobId,databaseName,procedureName,tText,rowCt,stepCt,'Done') into rtnCd;
		
		perform i2b2_fill_in_tree(TrialId, r_addNodes.leaf_node, jobID);

	END LOOP;  
	
--	update concept_cd for nodes, this is done to make the next insert easier
	begin
	update tm_wz.wt_RNA_SEQ_nodes t
	set concept_cd=(select c.concept_cd from i2b2demodata.concept_dimension c
	                where c.concept_path = t.leaf_node
				   )
    where exists
         (select 1 from i2b2demodata.concept_dimension x
	                where x.concept_path = t.leaf_node
				   )
	  and coalesce(t.concept_cd::text, '') = '';
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
	select cz_write_audit(jobId,databaseName,procedureName,'Update wt_RNA_SEQ_nodes with newly created concept_cds',rowCt,stepCt,'Done') into rtnCd;

  --Load the DE_SUBJECT_SAMPLE_MAPPING from wt_subject_RNA_sequencing_data

  --PATIENT_ID      = PATIENT_ID (SAME AS ID ON THE PATIENT_DIMENSION)
  --SITE_ID         = site_id
  --SUBJECT_ID      = subject_id
  --SUBJECT_TYPE    = NULL
  --CONCEPT_CODE    = from LEAF records in wt_RNA_SEQ_nodes
  --SAMPLE_TYPE    	= TISSUE_TYPE
  --SAMPLE_TYPE_CD  = concept_cd from TISSUETYPE records in wt_RNA_SEQ_nodes
  --TRIAL_NAME      = TRIAL_NAME
  --TIMEPOINT		= attribute_2
  --TIMEPOINT_CD	= concept_cd from ATTR2 records in wt_RNA_SEQ_nodes
  --TISSUE_TYPE     = attribute_1
  --TISSUE_TYPE_CD  = concept_cd from ATTR1 records in wt_RNA_SEQ_nodes
  --PLATFORM        = RNA_sequencing_AFFYMETRIX - this is required by ui code
  --PLATFORM_CD     = concept_cd from PLATFORM records in wt_RNA_SEQ_nodes
  --DATA_UID		= concatenation of concept_cd-patient_num
  --GPL_ID			= platform from wt_subject_RNA_SEQ_data
  --CATEGORY_CD		= category_cd that generated ontology
  --SAMPLE_ID		= id of sample (trial:S:[site_id]:subject_id:sample_cd) from patient_dimension, may be the same as patient_num
  --SAMPLE_CD		= sample_cd
  --SOURCE_CD		= sourceCd
  
  --ASSAY_ID        = generated by trigger
	begin
	insert into deapp.de_subject_sample_mapping(
	partition_id
	,patient_id
	,site_id
	,subject_id
	,subject_type
	,concept_code
	,assay_id
	,sample_type
	,sample_type_cd
	,trial_name
	,timepoint
	,timepoint_cd
	,tissue_type
	,tissue_type_cd
	,platform
	,platform_cd
	,data_uid
	,gpl_id
	,sample_id
	,sample_cd
	,category_cd
	,source_cd
	,omic_source_study
	,omic_patient_id
    )
	select partitionId
		  ,t.patient_id
		  ,t.site_id
		  ,t.subject_id
		  ,t.subject_type
		  ,t.concept_code
		  ,nextval('deapp.seq_assay_id')
		  ,t.sample_type
		  ,t.sample_type_cd
		  ,t.trial_name
		  ,t.timepoint
		  ,t.timepoint_cd
		  ,t.tissue_type
		  ,t.tissue_type_cd
		  ,t.platform
		  ,t.platform_cd
		  ,t.data_uid
		  ,t.gpl_id
		  ,t.sample_id
		  ,t.sample_cd
		  ,t.category_cd
		  ,t.source_cd
		  ,t.omic_source_study
		  ,t.omic_patient_id
	from (select distinct b.patient_num as patient_id
			  ,a.site_id
			  ,a.subject_id
			  ,null as subject_type
			  ,ln.concept_cd as concept_code
			  ,a.tissue_type as sample_type
			  ,ttp.concept_cd as sample_type_cd
			  ,a.trial_name
			  ,a.attribute_2 as timepoint
			  ,a2.concept_cd as timepoint_cd
			  ,a.attribute_1 as tissue_type
			  ,a1.concept_cd as tissue_type_cd
			  ,'RNA_AFFYMETRIX' as platform
			  ,pn.concept_cd as platform_cd
			  ,ln.concept_cd || '-' || b.patient_num::text as data_uid
			  ,a.platform as gpl_id
			  ,coalesce(sid.patient_num,b.patient_num) as sample_id
			  ,a.sample_cd
			  ,coalesce(a.category_cd,'Biomarker_Data+RNA_SEQ+PLATFORM+TISSUETYPE+ATTR1+ATTR2') as category_cd
			  ,a.source_cd
			  ,TrialId as omic_source_study
			  ,b.patient_num as omic_patient_id
		from tm_lz.lt_src_RNA_SEQ_subj_samp_map a		
		--Joining to Pat_dim to ensure the ID's match. If not I2B2 won't work.
		inner join i2b2demodata.patient_dimension b
		  on regexp_replace(TrialID || ':' || coalesce(a.site_id,'') || ':' || a.subject_id,'(::){1,}', ':', 'g') = b.sourcesystem_cd
		inner join tm_wz.wt_RNA_SEQ_nodes ln
			on a.platform = ln.platform
			and a.tissue_type = ln.tissue_type
			and coalesce(a.attribute_1,'@') = coalesce(ln.attribute_1,'@')
			and coalesce(a.attribute_2,'@') = coalesce(ln.attribute_2,'@')
			and ln.node_type = 'LEAF'
		inner join tm_wz.wt_RNA_SEQ_nodes pn
			on a.platform = pn.platform
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'PLATFORM')+8),'TISSUETYPE') > 1 then a.tissue_type else '@' end = coalesce(pn.tissue_type,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'PLATFORM')+8),'ATTR1') > 1 then a.attribute_1 else '@' end = coalesce(pn.attribute_1,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'PLATFORM')+8),'ATTR2') > 1 then a.attribute_2 else '@' end = coalesce(pn.attribute_2,'@')
			and pn.node_type = 'PLATFORM'	  
		left outer join tm_wz.wt_RNA_SEQ_nodes ttp
			on a.tissue_type = ttp.tissue_type
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'TISSUETYPE')+10),'PLATFORM') > 1 then a.platform else '@' end = coalesce(ttp.platform,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'TISSUETYPE')+10),'ATTR1') > 1 then a.attribute_1 else '@' end = coalesce(ttp.attribute_1,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'TISSUETYPE')+10),'ATTR2') > 1 then a.attribute_2 else '@' end = coalesce(ttp.attribute_2,'@')
			and ttp.node_type = 'TISSUETYPE'		  
		left outer join tm_wz.wt_RNA_SEQ_nodes a1
			on a.attribute_1 = a1.attribute_1
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'ATTR1')+5),'PLATFORM') > 1 then a.platform else '@' end = coalesce(a1.platform,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'ATTR1')+5),'TISSUETYPE') > 1 then a.tissue_type else '@' end = coalesce(a1.tissue_type,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'ATTR1')+5),'ATTR2') > 1 then a.attribute_2 else '@' end = coalesce(a1.attribute_2,'@')
			and a1.node_type = 'ATTR1'		  
		left outer join tm_wz.wt_RNA_SEQ_nodes a2
			on a.attribute_2 = a1.attribute_2
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'ATTR2')+5),'PLATFORM') > 1 then a.platform else '@' end = coalesce(a2.platform,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'ATTR2')+5),'TISSUETYPE') > 1 then a.tissue_type else '@' end = coalesce(a2.tissue_type,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'ATTR2')+5),'ATTR1') > 1 then a.attribute_1 else '@' end = coalesce(a2.attribute_1,'@')
			and a2.node_type = 'ATTR2'			  
		left outer join i2b2demodata.patient_dimension sid
			on regexp_replace(TrialID || ':' || coalesce(a.site_id,'') || ':' || a.subject_id,'(::){1,}', ':','g') = sid.sourcesystem_cd
		where a.trial_name = TrialID
		  and a.source_cd = sourceCD
		  and  (ln.concept_cd IS NOT NULL AND ln.concept_cd::text <> '')) as t;
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
	select cz_write_audit(jobId,databaseName,procedureName,'Insert trial into DEAPP de_subject_sample_mapping',rowCt,stepCt,'Done') into rtnCd;

--	Insert records for patients and samples into observation_fact
	begin
	insert into i2b2demodata.observation_fact
    (patient_num
	,concept_cd
	,modifier_cd
	,valtype_cd
	,tval_char
	,sourcesystem_cd
	,import_date
	,valueflag_cd
	,provider_id
	,location_cd
	,units_cd
        ,INSTANCE_NUM
    )
    select distinct m.patient_id
		  ,m.concept_code
		  ,'@'
		  ,'T' -- Text data type
		  ,'E'  --Stands for Equals for Text Types
		  ,m.trial_name
		  ,LOCALTIMESTAMP
		  ,'@'
		  ,'@'
		  ,'@'
		  ,'' -- no units available
                  ,1
    from  deapp.de_subject_sample_mapping m
    where m.trial_name = TrialID 
	  and m.source_cd = sourceCD
      and m.platform = 'RNA_AFFYMETRIX';
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
	select cz_write_audit(jobId,databaseName,procedureName,'Insert patient facts into I2B2DEMODATA observation_fact',rowCt,stepCt,'Done') into rtnCd;
  
	--	Insert sample facts 
	begin
	insert into i2b2demodata.observation_fact
    (patient_num
	,concept_cd
	,modifier_cd
	,valtype_cd
	,tval_char
	,sourcesystem_cd
	,import_date
	,valueflag_cd
	,provider_id
	,location_cd
	,units_cd
        ,INSTANCE_NUM
    )
    select distinct m.sample_id
		  ,m.concept_code
		  ,m.trial_name
		  ,'T' -- Text data type
		  ,'E'  --Stands for Equals for Text Types
		  ,m.trial_name
		  ,LOCALTIMESTAMP
		  ,'@'
		  ,'@'
		  ,'@'
		  ,'' -- no units available
                  ,1
    from  deapp.de_subject_sample_mapping m
    where m.trial_name = TrialID 
	  and m.source_cd = sourceCd
      and m.platform = 'RNA_AFFYMETRIX'
	  and m.patient_id != m.sample_id;
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
	select cz_write_audit(jobId,databaseName,procedureName,'Insert sample facts into I2B2DEMODATA observation_fact',rowCt,stepCt,'Done') into rtnCd;
    
	--Update I2b2 for correct data type
    begin

     update i2b2metadata.i2b2 t
    set c_columndatatype = 'T', c_metadataxml = null, c_visualattributes='FA'
    where t.c_basecode in (select distinct x.concept_cd from tm_wz.wt_RNA_SEQ_nodes x);
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
    select cz_write_audit(jobId,databaseName,procedureName,'Initialize data_type and xml in i2b2',rowCt,stepCt,'Done') into rtnCd;
    
 ---INSERT sample_dimension
	begin
      INSERT INTO I2B2DEMODATA.SAMPLE_DIMENSION(SAMPLE_CD)
         SELECT DISTINCT SAMPLE_CD FROM
           DEAPP.DE_SUBJECT_SAMPLE_MAPPING WHERE SAMPLE_CD NOT IN (SELECT SAMPLE_CD FROM I2B2DEMODATA.SAMPLE_DIMENSION) ;
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
    select cz_write_audit(jobId,databaseName,procedureName,'insert distinct sample_cd in sample_dimension from de_subject_sample_mapping',rowCt,stepCt,'Done') into rtnCd;

    ---- update c_metedataxml in i2b2
    begin
       for ul in uploadI2b2
        loop
     update i2b2metadata.i2b2 n
    SET c_columndatatype = 'T',
      --Static XML String
        c_metadataxml =  ('<?xml version="1.0"?><ValueMetadata><Version>3.02</Version><CreationDateTime>08/14/2008 01:22:59</CreationDateTime><TestID></TestID><TestName></TestName><DataType>PosFloat</DataType><CodeType></CodeType><Loinc></Loinc><Flagstouse></Flagstouse><Oktousevalues>Y</Oktousevalues><MaxStringLength></MaxStringLength><LowofLowValue>0</LowofLowValue>
                <HighofLowValue>0</HighofLowValue><LowofHighValue>100</LowofHighValue>100<HighofHighValue>100</HighofHighValue>
                <LowofToxicValue></LowofToxicValue><HighofToxicValue></HighofToxicValue>
                <EnumValues></EnumValues><CommentsDeterminingExclusion><Com></Com></CommentsDeterminingExclusion>
                <UnitValues><NormalUnits>ratio</NormalUnits><EqualUnits></EqualUnits>
                <ExcludingUnits></ExcludingUnits><ConvertingUnits><Units></Units><MultiplyingFactor></MultiplyingFactor>
                </ConvertingUnits></UnitValues><Analysis><Enums /><Counts />
                <New /></Analysis>'||(select xmlelement(name "SeriesMeta",xmlforest(m.display_value as "Value",m.display_unit as "Unit",m.display_label as "DisplayName")) as hi
      from tm_lz.lt_src_rna_display_mapping m where m.category_cd=ul.category_cd)||
                '</ValueMetadata>') where n.c_fullname=(select leaf_node from tm_wz.wt_RNA_SEQ_nodes where category_cd=ul.category_cd and leaf_node=n.c_fullname);
                
                end loop;
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
    select cz_write_audit(jobId,databaseName,procedureName,'Update c_columndatatype and c_metadataxml for numeric data types in I2B2METADATA i2b2',rowCt,stepCt,'Done') into rtnCd;

    --UPDATE VISUAL ATTRIBUTES for Leaf Active (Default is folder)
	begin
    update i2b2metadata.i2b2 a
    set c_visualattributes = 'LAH'
    where a.c_basecode in (select distinct x.concept_code from deapp.de_subject_sample_mapping x
                           where x.trial_name = TrialId
                             and x.platform = 'RNA_AFFYMETRIX'
                             and x.concept_code is not null);
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
    select cz_write_audit(jobId,databaseName,procedureName,'Update visual attributes for leaf nodes in I2B2METADATA i2b2',rowCt,stepCt,'Done') into rtnCd;
 
	begin
        update i2b2metadata.i2b2 a
    set c_visualattributes='FAS'
        where a.c_fullname = substr(topNode,1,instr(topNode,'\',1,3));
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
    select cz_write_audit(jobId,databaseName,procedureName,'Update visual attributes for study node in I2B2METADATA i2b2',rowCt,stepCt,'Done') into rtnCd;
	
	begin
   insert into probeset_deapp
   (
   probeset,
   platform 
   )select distinct s.probeset
               ,m.platform               
            from tm_lz.lt_src_rna_seq_data s,
                 tm_lz.lt_src_RNA_SEQ_subj_samp_map m  
                 where s.trial_name=m.trial_name  
                   and not exists
		 (select 1 from probeset_deapp x
		  where m.platform = x.platform
		    and s.probeset = x.probeset);
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
	select cz_write_audit(jobId,databaseName,procedureName,'Insert new probesets into probeset_deapp',rowCt,stepCt,'Done') into rtnCd;
        
  --Build concept Counts
  --Also marks any i2B2 records with no underlying data as Hidden, need to do at Trial level because there may be multiple platform and there is no longer
  -- a unique top-level node for RNA_sequencing data
	begin
    perform i2b2_create_concept_counts(topNode ,jobID );
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
	select cz_write_audit(jobId,databaseName,procedureName,'Create concept counts',rowCt,stepCt,'Done') into rtnCd;
	
	--	delete each node that is hidden
	 FOR r_delNodes in delNodes Loop

    --	deletes hidden nodes for a trial one at a time
		begin
		perform i2b2_delete_1_node(r_delNodes.c_fullname);
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
		tText := 'Deleted node: ' || r_delNodes.c_fullname;
		
		select cz_write_audit(jobId,databaseName,procedureName,tText,rowCt,stepCt,'Done') into rtnCd;

	END LOOP;  	

  --Reload Security: Inserts one record for every I2B2 record into the security table
	begin
    perform i2b2_load_security_data(jobId);
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
	select cz_write_audit(jobId,databaseName,procedureName,'Load security data',rowCt,stepCt,'Done') into rtnCd;

--	tag data with probeset_id from reference.probeset_deapp
  
	EXECUTE ('truncate table tm_wz.wt_subject_rna_probeset');
	
	--	note: assay_id represents a unique subject/site/sample
	begin
	insert into tm_wz.wt_subject_rna_probeset
	(probeset_id
--	,expr_id
	,intensity_value
	,patient_id
--	,sample_cd
--	,subject_id
	,trial_name
	,assay_id
	) select md.probeset
--		  ,sd.sample_cd
		 , avg(md.intensity_value) as intensity_value
		  ,sd.patient_id
--		  ,sd.sample_cd
--		  ,sd.subject_id
		 ,TrialId as trial_name
		  ,sd.assay_id
	from deapp.de_subject_sample_mapping sd
		,tm_lz.lt_src_RNA_SEQ_data md   
		,probeset_deapp gs
	where sd.sample_cd = md.expr_id
	  and sd.platform = 'RNA_AFFYMETRIX'
	  and sd.trial_name = TrialId
	  and sd.source_cd = sourceCd
	and md.probeset = gs.probeset
	  and (CASE WHEN dataType = 'R' THEN sign(md.intensity_value) ELSE 1 END) = 1  --	take only >0 for dataType R
	  and sd.subject_id in (select subject_id from tm_lz.lt_src_rna_seq_subj_samp_map)
	group by md.probeset
		  ,sd.patient_id
		  ,sd.assay_id;
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
	pExists := rowCt;
	
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Insert into DEAPP wt_subject_rna_probeset',rowCt,stepCt,'Done') into rtnCd;
	
	--	insert into de_subject_rna_data when dataType is T (transformed)

	if dataType = 'T' then
	begin
		insert into deapp.de_subject_rna_data
		(trial_source
		,probeset_id
		,assay_id
		,patient_id
		,trial_name
		,zscore
		)
		select TrialId || ':' || sourceCd
			  ,probeset_id
			  ,assay_id
			  ,patient_id
			  ,trial_name
			  ,case when intensity_value < -2.5
			        then -2.5
					when intensity_value > 2.5
					then 2.5
					else intensity_value
			   end as zscore
		from tm_wz.wt_subject_rna_probeset 
		where trial_name = TrialID;
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
		select cz_write_audit(jobId,databaseName,procedureName,'Insert transformed into DEAPP de_subject_rna_data',rowCt,stepCt,'Done') into rtnCd;
	
	else
		
	--	Calculate ZScores and insert data into de_subject_rna_data.  The 'L' parameter indicates that the RNA_sequencing  data will be selected from
	--	wt_subject_RNA_seq_probeset as part of a Load.  

		if dataType = 'R' or dataType = 'L' then
			begin
			perform I2B2_RNA_SEQ_ZSCORE_CALC(TrialID, partitionName, partitionindx,partitioniD,'L',jobId,dataType,logBase,sourceCD);
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
			select cz_write_audit(jobId,databaseName,procedureName,'Calculate Z-Score',rowCt,stepCt,'Done') into rtnCd;
		end if;
	
	end if;
	
    ---Cleanup OVERALL JOB if this proc is being run standalone
	
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'End i2b2_process_RNA_SEQ_data',0,stepCt,'Done') into rtnCd;

	IF newJobFlag = 1
	THEN
		select cz_end_audit (jobID, 'SUCCESS') into rtnCd;
	END IF;
	
	return 0;
END;
 
$$;

