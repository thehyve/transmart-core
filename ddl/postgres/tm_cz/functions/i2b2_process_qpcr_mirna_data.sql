--
-- Name: i2b2_process_qpcr_mirna_data(character varying, character varying, character varying, character varying, numeric, character varying, numeric, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_process_qpcr_mirna_data(trial_id character varying, top_node character varying, data_type character varying DEFAULT 'R'::character varying, source_cd character varying DEFAULT 'STD'::character varying, log_base numeric DEFAULT 2, secure_study character varying DEFAULT NULL::character varying, currentjobid numeric DEFAULT NULL::numeric, mirna_type character varying DEFAULT NULL::character varying) RETURNS numeric
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
/*************************************************************************

* This store procedure is for ETL for Sanofi to load  qpcr or seq miRNA data
* Date: 12/05/2013

******************************************************************/


--	***  NOTE ***
--	The input file columns are mapped to the following table columns.  This is done so that the javascript for the advanced workflows
--	selects the correct data for the dropdowns.

--		tissue_type	=>	sample_type
--		attribute_1	=>	tissue_type
--		atrribute_2	=>	timepoint	
Declare
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
  pExists		numeric;
  partTbl   	numeric;
  partExists 	numeric;
  sampleCt		numeric;
  idxExists 	numeric;
  logBase		numeric;
  pCount		integer;
  sCount		integer;
  tablespaceName	varchar(200);
  v_bio_experiment_id	numeric(18,0);
  mirnaType varchar(15);
 -- mirnaPlatform varchar(20);
  
    --Audit variables
  newJobFlag numeric(1);
  databaseName VARCHAR(100);
  procedureName VARCHAR(100);
  jobID numeric(18,0);
  stepCt numeric(18,0);
  rowCt	integer;
  errorNumber		character varying;
  errorMessage	character varying;
    
	addNodes CURSOR is
	select distinct t.leaf_node
          ,t.node_name
	from  wt_qpcr_mirna_nodes t
	where not exists
		 (select 1 from i2b2 x
		  where t.leaf_node = x.c_fullname);

 
--	cursor to define the path for delete_one_node  this will delete any nodes that are hidden after i2b2_create_concept_counts

  delNodes CURSOR for
  select distinct c_fullname 
  from  i2b2
  where c_fullname like topNode || '%'
    and substr(c_visualattributes,2,1) = 'H';
    --and c_visualattributes like '_H_';

    uploadI2b2 cursor for 
    select category_cd,display_value,display_label,display_unit from
    tm_lz.lt_src_mirna_display_mapping;



BEGIN
	TrialID := upper(trial_id);
	secureStudy := upper(secure_study);
	mirnaType:=upper(mirna_type);
	
	if (secureStudy not in ('Y','N') ) then
		secureStudy := 'Y';
	end if;
	
	topNode := REGEXP_REPLACE('\' || top_node || '\','(\\){2,}', '\', 'g');	
	select length(topNode)-length(replace(topNode,'\','')) into topLevel ;
	
	if data_type is null then
		dataType := 'R';
	else
		if data_type in ('R','T','L') then
			dataType := data_type;
		else
			dataType := 'R';
		end if;
	end if;

	logBase := log_base;
	sourceCd := upper(coalesce(source_cd,'STD'));

  --Set Audit Parameters
  newJobFlag := 0; -- False (Default)
  jobID := currentJobID;

  databaseName := 'TM_CZ';
  procedureName := 'I2B2_PROCESS_QPCR_MIRNA_DATA';
  --Audit JOB Initialization
  --If Job ID does not exist, then this is a single procedure run and we need to create it
  IF(jobID IS NULL or jobID < 1)
  THEN
    newJobFlag := 1; -- True
    select cz_start_audit (procedureName, databaseName) into jobID;
  END IF;
    	
	stepCt := 0;
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_process_qpcr_mirna_data',0,stepCt,'Done');
	
	--	Get count of records in LT_SRC_MIRNA_SUBJ_SAMP_MAP
	
	select count(*) into sCount
	from LT_SRC_MIRNA_SUBJ_SAMP_MAP;
	
	--	check if all subject_sample map records have a platform, If not, abort run
	
	select count(*) into pCount
	from LT_SRC_MIRNA_SUBJ_SAMP_MAP
	where platform is null;
	
	if pCount > 0 then
		perform cz_write_audit(jobId,databasename,procedurename,'Platform data missing from one or more subject_sample mapping records',1,stepCt,'ERROR');
		perform cz_error_handler(jobid,procedurename, '-1', 'Application raised error');
		perform cz_end_audit (jobId,'FAIL');
		return 161;
	end if;
  
  	--	check if platform exists in de_qpcr_mirna_annotation .  If not, abort run.
	
	select count(*) into pCount
	from deapp.de_qpcr_mirna_annotation
	where gpl_id in (select distinct m.platform from LT_SRC_MIRNA_SUBJ_SAMP_MAP m);
	
	if PCOUNT = 0 then
		perform cz_write_audit(jobId,databasename,procedurename,'Unmapped platform',1,stepCt,'ERROR');
		perform cz_error_handler(jobid,procedurename, '-1', 'Application raised error');
		perform cz_end_audit (jobId,'FAIL');
		return 162;
	end if;--mod
	
	select count(*) into pCount
	from DE_gpl_info
	where platform in (select distinct m.platform from LT_SRC_MIRNA_SUBJ_SAMP_MAP m);
	
	if PCOUNT = 0 then
		perform cz_write_audit(jobId,databasename,procedurename,'No platoform in de_gpl_info',1,stepCt,'ERROR');
		perform cz_error_handler(jobid,procedurename, '-1', 'Application raised error');
		perform cz_end_audit (jobId,'FAIL');
		return 163;
	end if;
		
	--	check if all subject_sample map records have a tissue_type, If not, abort run
	
	select count(*) into pCount
	from LT_SRC_MIRNA_SUBJ_SAMP_MAP
	where tissue_type is null;
	
	if pCount > 0 then
		perform cz_write_audit(jobId,databasename,procedurename,'Missing tissue',1,stepCt,'ERROR');
		perform cz_error_handler(jobid,procedurename, '-1', 'Application raised error');
		perform cz_end_audit (jobId,'FAIL');
		return 164;
	end if;
	
	--	check if there are multiple platforms, if yes, then platform must be supplied in LT_SRC_QPCR_MIRNA_DATA
	
	select count(*) into pCount
	from (select sample_cd
		  from LT_SRC_MIRNA_SUBJ_SAMP_MAP
		  group by sample_cd
		  having count(distinct platform) > 1) t;
	
	if pCount > 0 then
		perform cz_write_audit(jobId,databasename,procedurename,'Multiple platforms',1,stepCt,'ERROR');
		perform cz_error_handler(jobid,procedurename, '-1', 'Application raised error');
		perform cz_end_audit (jobId,'FAIL');
		return 165;
	end if;
		
	-- Get root_node from topNode
  
	select parse_nth_value(topNode, 2, '\') into RootNode ;
	
	select count(*) into pExists
	from table_access
	where c_name = rootNode;
	
	if pExists = 0 then
		perform tm_cz.i2b2_add_root_node(rootNode, jobId);
	end if;
	
	select c_hlevel into root_level
	from i2b2
	where c_name = RootNode;
	
	-- Get study name from topNode
  
	select parse_nth_value(topNode, topLevel, '\') into study_name ;
	
	--	Add any upper level nodes as needed
	
	tPath := REGEXP_REPLACE(replace(top_node,study_name,''),'(\\){2,}', '\', 'g');
	select length(tPath) - length(replace(tPath,'\','')) into pCount ;
	if pCount > 2 then
		perform i2b2_fill_in_tree(null, tPath, jobId);
	end if;

	--	uppercase study_id in lt_src_mirna_subj_samp_map in case curator forgot
	
	update LT_SRC_MIRNA_SUBJ_SAMP_MAP
	set trial_name=upper(trial_name);
	
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Uppercase trial_name in LT_SRC_MIRNA_SUBJ_SAMP_MAP',rowCt,stepCt,'Done');	
	
	--	create records in patient_dimension for subject_ids if they do not exist
	--	format of sourcesystem_cd:  trial:[site:]subject_cd

	begin
	insert into patient_dimension
    ( patient_num,
      sex_cd,
      age_in_years_num,
      race_cd,
      update_date,
      download_date,
      import_date,
      sourcesystem_cd
    )
    select nextval('seq_patient_num')
		  ,x.sex_cd
		  ,x.age_in_years_num
		  ,x.race_cd
		  ,current_timestamp
		  ,current_timestamp
		  ,current_timestamp
		  ,x.sourcesystem_cd
	from (select distinct 'Unknown' as sex_cd,
				 0 as age_in_years_num,
				 null as race_cd,
				 regexp_replace(TrialID || ':' || coalesce(s.site_id,'') || ':' || s.subject_id,'(::){1,}', ':', 'g') as sourcesystem_cd
		 from LT_SRC_MIRNA_SUBJ_SAMP_MAP s
		     ,de_gpl_info g
		 where s.subject_id is not null
		   and s.trial_name = TrialID
		   and s.source_cd = sourceCD
		   and s.platform = g.platform
		   and upper(g.marker_type) = mirnaType
		   and not exists
			  (select 1 from patient_dimension x
			   where x.sourcesystem_cd = 
				 regexp_replace(TrialID || ':' || coalesce(s.site_id,'') || ':' || s.subject_id,'(::){1,}', ':', 'g'))
		) x;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
			
	get diagnostics rowCt := ROW_COUNT;
	pCount := rowCt;
	
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Insert subjects to patient_dimension.',pCount,stepCt,'Done');
	
	begin
		perform i2b2_create_security_for_trial(TrialId, secureStudy, jobID);
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
	--	Delete existing observation_fact data, will be repopulated

	begin
	delete from observation_fact obf
	where obf.concept_cd in
		 (select distinct x.concept_code
		  from de_subject_sample_mapping x
		  where x.trial_name = TrialId
		    and coalesce(x.source_cd,'STD') = sourceCD
		    and x.platform = mirna_type);
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
	
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Delete data from observation_fact',rowCt,stepCt,'Done');
	
        begin
	delete from de_subject_mirna_data
	where trial_source = TrialId || ':' || sourceCd;
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Delete data from de_subject_mirna_data',rowCt,stepCt,'Done');
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	begin
	delete from deapp.DE_SUBJECT_SAMPLE_MAPPING d 
	where trial_name = TrialID 
	  and coalesce(d.source_cd,'STD') = sourceCd
	  and platform = mirna_type;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
	  
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Delete trial from DEAPP de_subject_sample_mapping',rowCt,stepCt,'Done');

	begin
		execute('truncate table tm_wz.WT_QPCR_MIRNA_NODES');
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	begin
		execute('truncate table tm_wz.WT_QPCR_MIRNA_NODE_VALUES');
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	begin
	insert into WT_QPCR_MIRNA_NODE_VALUES
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
				   ,g.title
    from LT_SRC_MIRNA_SUBJ_SAMP_MAP a
	    ,de_gpl_info g 
	where a.trial_name = TrialID
	  and coalesce(a.platform,'GPL570') = g.platform
	  and a.source_cd = sourceCD
	  and a.platform = g.platform
	  and upper(g.marker_type) = mirnaType
	  and g.title = (select min(x.title) from de_gpl_info x where coalesce(a.platform,'GPL570') = x.platform)
      -- and upper(g.organism) = 'HOMO SAPIENS'
	  ;
        exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
	
	--  and decode(dataType,'R',sign(a.intensity_value),1) = 1;	--	take all values when dataType T, only >0 for dataType R
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Insert node values into DEAPP wt_qpcr_mirna_node_values',rowCt,stepCt,'Done');
	
	begin
	insert into WT_QPCR_MIRNA_NODES
	(leaf_node
	,category_cd
	,platform
	,tissue_type
	,attribute_1
    ,attribute_2
	,node_type
	)
	select distinct topNode || regexp_replace(replace(replace(replace(replace(replace(replace(
	       category_cd,'PLATFORM',title),'ATTR1',coalesce(attribute_1, '')),'ATTR2',coalesce(attribute_2, '')),'TISSUETYPE',tissue_type),'+','\'),'_',' ') || '\','(\\){2,}', '\', 'g') 
		  ,category_cd
		  ,platform as platform
		  ,tissue_type
		  ,attribute_1 as attribute_1
          ,attribute_2 as attribute_2
		  ,'LEAF'
	from  WT_QPCR_MIRNA_NODE_VALUES;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
		   
    stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Create leaf nodes in DEAPP tmp_mirna_nodes',rowCt,stepCt,'Done');
	
	
	--	insert for platform node so platform concept can be populated
	begin
	insert into WT_QPCR_MIRNA_NODES
	(leaf_node
	,category_cd
	,platform
	,tissue_type
	,attribute_1
        ,attribute_2
	,node_type
	)
	select distinct topNode || regexp_replace(replace(replace(replace(replace(replace(replace(
	       substr(category_cd,1,instr(category_cd,'PLATFORM')+8),'PLATFORM',title),'ATTR1',coalesce(attribute_1, '')),'ATTR2',coalesce(attribute_2, '')),'TISSUETYPE',tissue_type),'+','\'),'_',' ') || '\',
		   '(\\){2,}', '\', 'g')
		  ,substr(category_cd,1,instr(category_cd,'PLATFORM')+8)
		  ,platform as platform
		  ,case when instr(substr(category_cd,1,instr(category_cd,'PLATFORM')+8),'TISSUETYPE') > 1 then tissue_type else null end as tissue_type
		  ,case when instr(substr(category_cd,1,instr(category_cd,'PLATFORM')+8),'ATTR1') > 1 then attribute_1 else null end as attribute_1
          ,case when instr(substr(category_cd,1,instr(category_cd,'PLATFORM')+8),'ATTR2') > 1 then attribute_2 else null end as attribute_2
		  ,'PLATFORM'
	from  WT_QPCR_MIRNA_NODE_VALUES;
		   
        stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Create platform nodes in wt_qpcr_mirna_nodes',rowCt,stepCt,'Done');
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
	
	--	insert for ATTR1 node so ATTR1 concept can be populated in tissue_type_cd
	begin
	insert into WT_QPCR_MIRNA_NODES
	(leaf_node
	,category_cd
	,platform
	,tissue_type
        ,attribute_1
	,attribute_2
	,node_type
	)
	select distinct topNode || regexp_replace(replace(replace(replace(replace(replace(replace(
	       substr(category_cd,1,instr(category_cd,'ATTR1')+5),'PLATFORM',title),'ATTR1',coalesce(attribute_1, '')),'ATTR2',coalesce(attribute_2, '')),'TISSUETYPE',tissue_type),'+','\'),'_',' ') || '\',
		   '(\\){2,}', '\', 'g')
		  ,substr(category_cd,1,instr(category_cd,'ATTR1')+5)
		  ,case when instr(substr(category_cd,1,instr(category_cd,'ATTR1')+5),'PLATFORM') > 1 then platform else null end as platform
		  ,case when instr(substr(category_cd,1,instr(category_cd,'ATTR1')+5),'TISSUETYPE') > 1 then tissue_type else null end as tissue_type
		  ,attribute_1 as attribute_1
          ,case when instr(substr(category_cd,1,instr(category_cd,'ATTR1')+5),'ATTR2') > 1 then attribute_2 else null end as attribute_2
		  ,'ATTR1'
	from  WT_QPCR_MIRNA_NODE_VALUES
	where category_cd like '%ATTR1%'
        and attribute_1 is not null;
        exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
		   
        stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Create ATTR1 nodes in WT_QPCR_MIRNA_NODES',rowCt,stepCt,'Done');
	
	
	--	insert for ATTR2 node so ATTR2 concept can be populated in timepoint_cd

	begin
	insert into WT_QPCR_MIRNA_NODES
	(leaf_node
	,category_cd
	,platform
	,tissue_type
        ,attribute_1
	,attribute_2
	,node_type
	)
	select distinct topNode || regexp_replace(replace(replace(replace(replace(replace(replace(
	       substr(category_cd,1,instr(category_cd,'ATTR2')+5),'PLATFORM',title),'ATTR1',coalesce(attribute_1, '')),'ATTR2',coalesce(attribute_2, '')),'TISSUETYPE',tissue_type),'+','\'),'_',' ') || '\',
		   '(\\){2,}', '\', 'g')
		  ,substr(category_cd,1,instr(category_cd,'ATTR2')+5)
		  ,case when instr(substr(category_cd,1,instr(category_cd,'ATTR2')+5),'PLATFORM') > 1 then platform else null end as platform
		  ,case when instr(substr(category_cd,1,instr(category_cd,'ATTR1')+5),'TISSUETYPE') > 1 then tissue_type else null end as tissue_type
          ,case when instr(substr(category_cd,1,instr(category_cd,'ATTR2')+5),'ATTR1') > 1 then attribute_1 else null end as attribute_1
		  ,attribute_2 as attribute_2
		  ,'ATTR2'
	from  WT_QPCR_MIRNA_NODE_VALUES
	where category_cd like '%ATTR2%'
	  and attribute_2 is not null;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
		   
        stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Create ATTR2 nodes in WT_QPCR_MIRNA_NODES',rowCt,stepCt,'Done');
	
	
	--	insert for tissue_type node so sample_type_cd can be populated
	begin
	insert into WT_QPCR_MIRNA_NODES
	(leaf_node
	,category_cd
	,platform
	,tissue_type
	,attribute_1
        ,attribute_2
	,node_type
	)
	select distinct topNode || regexp_replace(replace(replace(replace(replace(replace(replace(
	       substr(category_cd,1,instr(category_cd,'TISSUETYPE')+10),'PLATFORM',title),'ATTR1',coalesce(attribute_1, '')),'ATTR2',coalesce(attribute_2, '')),'TISSUETYPE',tissue_type),'+','\'),'_',' ') || '\',
		   '(\\){2,}', '\', 'g')
		  ,substr(category_cd,1,instr(category_cd,'TISSUETYPE')+10)
		  ,case when instr(substr(category_cd,1,instr(category_cd,'TISSUETYPE')+10),'PLATFORM') > 1 then platform else null end as platform
		  ,tissue_type as tissue_type
		  ,case when instr(substr(category_cd,1,instr(category_cd,'TISSUETYPE')+10),'ATTR1') > 1 then attribute_1 else null end as attribute_1
          ,case when instr(substr(category_cd,1,instr(category_cd,'TISSUETYPE')+10),'ATTR2') > 1 then attribute_2 else null end as attribute_2
		  ,'TISSUETYPE'
	from  WT_QPCR_MIRNA_NODE_VALUES
	where category_cd like '%TISSUETYPE%';
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
	 
        stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Create ATTR2 nodes in wt_qpcr_mirna_nodes',rowCt,stepCt,'Done');
	
	begin			
	update WT_QPCR_MIRNA_NODES
	set node_name=parse_nth_value(leaf_node,length(leaf_node)-length(replace(leaf_node,'\','')),'\');
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
	
        stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Updated node_name in DEAPP tmp_mirna_nodes',rowCt,stepCt,'Done');
	
		
--	add leaf nodes for miRNA data  The cursor will only add nodes that do not already exist.

	 FOR r_addNodes in addNodes Loop

    --Add nodes for all types (ALSO DELETES EXISTING NODE)
		begin
			perform i2b2_add_node(TrialID, r_addNodes.leaf_node, r_addNodes.node_name, jobId);
			stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
			tText := 'Added Leaf Node: ' || r_addNodes.leaf_node || '  Name: ' || r_addNodes.node_name;
		
			perform cz_write_audit(jobId,databaseName,procedureName,tText,rowCt,stepCt,'Done');
		
			perform i2b2_fill_in_tree(TrialId, r_addNodes.leaf_node, jobID);
		exception
		when others then
			errorNumber := SQLSTATE;
			errorMessage := SQLERRM;
			perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
			perform tm_cz.cz_end_audit (jobID, 'FAIL');
			return -16;
		end;

	END LOOP;  
	
	begin
	update WT_QPCR_MIRNA_NODES t
	set concept_cd=(select c.concept_cd from concept_dimension c
	                where c.concept_path = t.leaf_node limit 1)
        where exists
         (select 1 from concept_dimension x
	                where x.concept_path = t.leaf_node
				   )
	  and t.concept_cd is null;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
	
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Update WT_QPCR_MIRNA_NODES with newly created concept_cds',rowCt,stepCt,'Done');
	
	 
	
  --Load the DE_SUBJECT_SAMPLE_MAPPING from wt_subject_mirna_data

  --PATIENT_ID      = PATIENT_ID (SAME AS ID ON THE PATIENT_DIMENSION)
  --SITE_ID         = site_id
  --SUBJECT_ID      = subject_id
  --SUBJECT_TYPE    = NULL
  --CONCEPT_CODE    = from LEAF records in wt_mirna_nodes
  --SAMPLE_TYPE    	= TISSUE_TYPE
  --SAMPLE_TYPE_CD  = concept_cd from TISSUETYPE records in wt_mirna_nodes
  --TRIAL_NAME      = TRIAL_NAME
  --TIMEPOINT		= attribute_2
  --TIMEPOINT_CD	= concept_cd from ATTR2 records in wt_mirna_nodes
  --TISSUE_TYPE     = attribute_1
  --TISSUE_TYPE_CD  = concept_cd from ATTR1 records in wt_mirna_nodes
  --PLATFORM        = MIRNA_AFFYMETRIX - this is required by ui code
  --PLATFORM_CD     = concept_cd from PLATFORM records in wt_qpcr_mirna_nodes
  --DATA_UID		= concatenation of concept_cd-patient_num
  --GPL_ID			= platform from wt_subject_mirna_data
  --CATEGORY_CD		= category_cd that generated ontology
  --SAMPLE_ID		= id of sample (trial:S:[site_id]:subject_id:sample_cd) from patient_dimension, may be the same as patient_num
  --SAMPLE_CD		= sample_cd
  --SOURCE_CD		= sourceCd
  
  --ASSAY_ID        = generated by trigger

	begin
	insert into de_subject_sample_mapping
	(patient_id
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
	select t.patient_id
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
			  ,mirna_type as platform
			  ,pn.concept_cd as platform_cd
			  ,ln.concept_cd || '-' || b.patient_num::varchar as data_uid
			  ,a.platform as gpl_id
			  ,coalesce(sid.patient_num,b.patient_num) as sample_id
			  ,a.sample_cd
			  ,coalesce(a.category_cd,'Biomarker_Data+QPCR_MIRNA+PLATFORM+TISSUETYPE+ATTR1+ATTR2') as category_cd
			  ,a.source_cd
			  ,TrialId as omic_source_study
			  ,b.patient_num as omic_patient_id
		from lt_src_mirna_subj_samp_map a		
		--Joining to Pat_dim to ensure the ID's match. If not I2B2 won't work.
		inner join patient_dimension b
		  on regexp_replace(TrialID || ':' || coalesce(a.site_id,'') || ':' || a.subject_id,'(::){1,}', ':', 'g') = b.sourcesystem_cd
		inner join WT_QPCR_MIRNA_NODES ln
			on a.platform = ln.platform
			and a.category_cd=ln.category_cd
			and a.tissue_type = ln.tissue_type
			and coalesce(a.attribute_1,'@') = coalesce(ln.attribute_1,'@')
			and coalesce(a.attribute_2,'@') = coalesce(ln.attribute_2,'@')
			and ln.node_type = 'LEAF'
		inner join WT_QPCR_MIRNA_NODES pn
			on a.platform = pn.platform
			and  pn.category_cd=substr(a.category_cd,1,instr(a.category_cd,'PLATFORM')+8)
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'PLATFORM')+8),'TISSUETYPE') > 1 then a.tissue_type else '@' end = coalesce(pn.tissue_type,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'PLATFORM')+8),'ATTR1') > 1 then a.attribute_1 else '@' end = coalesce(pn.attribute_1,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'PLATFORM')+8),'ATTR2') > 1 then a.attribute_2 else '@' end = coalesce(pn.attribute_2,'@')
			and pn.node_type = 'PLATFORM'	  
		left outer join WT_QPCR_MIRNA_NODES ttp
			on a.tissue_type = ttp.tissue_type
			and ttp.category_cd=substr(a.category_cd,1,instr(a.category_cd,'TISSUETYPE')+10)
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'TISSUETYPE')+10),'PLATFORM') > 1 then a.platform else '@' end = coalesce(ttp.platform,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'TISSUETYPE')+10),'ATTR1') > 1 then a.attribute_1 else '@' end = coalesce(ttp.attribute_1,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'TISSUETYPE')+10),'ATTR2') > 1 then a.attribute_2 else '@' end = coalesce(ttp.attribute_2,'@')
			and ttp.node_type = 'TISSUETYPE'		  
		left outer join WT_QPCR_MIRNA_NODES a1
			on a.attribute_1 = a1.attribute_1
			and a1.category_cd=substr(a.category_cd,1,instr(a.category_cd,'ATTR1')+5)
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'ATTR1')+5),'PLATFORM') > 1 then a.platform else '@' end = coalesce(a1.platform,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'ATTR1')+5),'TISSUETYPE') > 1 then a.tissue_type else '@' end = coalesce(a1.tissue_type,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'ATTR1')+5),'ATTR2') > 1 then a.attribute_2 else '@' end = coalesce(a1.attribute_2,'@')
			and a1.node_type = 'ATTR1'		  
		left outer join WT_QPCR_MIRNA_NODES a2
			on a.attribute_2 = a1.attribute_2
			and a2.category_cd=substr(a.category_cd,1,instr(a.category_cd,'ATTR2')+5)
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'ATTR2')+5),'PLATFORM') > 1 then a.platform else '@' end = coalesce(a2.platform,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'ATTR2')+5),'TISSUETYPE') > 1 then a.tissue_type else '@' end = coalesce(a2.tissue_type,'@')
			and case when instr(substr(a.category_cd,1,instr(a.category_cd,'ATTR2')+5),'ATTR1') > 1 then a.attribute_1 else '@' end = coalesce(a2.attribute_1,'@')
			and a2.node_type = 'ATTR2'			  
		left outer join patient_dimension sid
			on regexp_replace(TrialID || ':' || coalesce(a.site_id,'') || ':' || a.subject_id,'(::){1,}', ':','g') = sid.sourcesystem_cd
		where a.trial_name = TrialID
		  and a.source_cd = sourceCD
		  and  ln.concept_cd is not null) t;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Insert trial into DEAPP de_subject_sample_mapping',rowCt,stepCt,'Done');

--	Insert records for patients and samples into observation_fact
	begin
	insert into observation_fact
        (patient_num
	,concept_cd
	,modifier_cd
	,valtype_cd
	,tval_char
	,nval_num
	,sourcesystem_cd
	,import_date
	,valueflag_cd
	,provider_id
	,location_cd
	,units_cd
        ,sample_cd
        ,INSTANCE_NUM
        )
        select distinct m.patient_id
		  ,m.concept_code
		  ,'@'
		  ,'T' -- Text data type
		  ,'E'  --Stands for Equals for Text Types
		  ,null::numeric	--	not numeric for qpcr_mirna
		  ,m.trial_name
		  ,current_timestamp
		  ,'@'
		  ,'@'
		  ,'@'
		  ,'' -- no units available
                   ,m.sample_cd
                  ,1
        from  de_subject_sample_mapping m
        where m.trial_name = TrialID 
        and m.source_cd = sourceCD
        and m.platform = mirna_type;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
        stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Insert patient facts into I2B2DEMODATA observation_fact',rowCt,stepCt,'Done');
    
	--	Insert sample facts 
	begin
	insert into observation_fact
    (patient_num
	,concept_cd
	,modifier_cd
	,valtype_cd
	,tval_char
	,nval_num
	,sourcesystem_cd
	,import_date
	,valueflag_cd
	,provider_id
	,location_cd
	,units_cd
        ,sample_cd
        ,INSTANCE_NUM
    )
    select distinct m.sample_id
		  ,m.concept_code
		  ,m.trial_name
		  ,'T' -- Text data type
		  ,'E'  --Stands for Equals for Text Types
		  ,null::numeric--	not numeric for miRNA
		  ,m.trial_name
		  ,current_timestamp
		  ,'@'
		  ,'@'
		  ,'@'
		  ,'' -- no units available
                  ,m.sample_cd
                  ,1
    from  de_subject_sample_mapping m
    where m.trial_name = TrialID 
    and m.source_cd = sourceCd
    and m.platform = mirna_type
    and m.patient_id != m.sample_id;
    exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	--Update I2b2 for correct data type
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Insert sample facts into I2B2DEMODATA observation_fact',rowCt,stepCt,'Done');
    
	begin
	update i2b2 t
	set c_columndatatype = 'T', c_metadataxml = null, c_visualattributes='FA'
	where t.c_basecode in (select distinct x.concept_cd from WT_QPCR_MIRNA_NODES x);
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
		
	 ---INSERT sample_dimension
      stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
      perform cz_write_audit(jobId,databaseName,procedureName,'Initialize data_type and xml in i2b2',rowCt,stepCt,'Done');
      
	  begin
      INSERT INTO I2B2DEMODATA.SAMPLE_DIMENSION(SAMPLE_CD) 
         SELECT DISTINCT SAMPLE_CD FROM 
           DEAPP.DE_SUBJECT_SAMPLE_MAPPING WHERE SAMPLE_CD NOT IN (SELECT SAMPLE_CD FROM I2B2DEMODATA.SAMPLE_DIMENSION) ;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'insert distinct sample_cd in sample_dimension from de_subject_sample_mapping',rowCt,stepCt,'Done');
	

    ---- update c_metedataxml in i2b2
 begin   
   for ul in uploadI2b2 loop
	 update i2b2 n
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
      from tm_lz.lt_src_mirna_display_mapping m where m.category_cd=ul.category_cd)||
                '</ValueMetadata>') where n.c_fullname=(select leaf_node from WT_QPCR_MIRNA_NODES where category_cd=ul.category_cd and leaf_node=n.c_fullname);
     end loop;
      exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
		  
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Update c_columndatatype and c_metadataxml for numeric data types in I2B2METADATA i2b2',rowCt,stepCt,'Done');

	--UPDATE VISUAL ATTRIBUTES for Leaf Active (Default is folder)
	begin
	update i2b2 a
        set c_visualattributes = 'LAH'
	where a.c_basecode in (select distinct x.concept_code from de_subject_sample_mapping x
						   where x.trial_name = TrialId
						     and x.platform = mirna_type
							 and x.concept_code is not null);
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;

	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Update visual attributes for leaf nodes in I2B2METADATA i2b2',rowCt,stepCt,'Done');

	begin
    update i2b2 a
	set c_visualattributes='FAS'
        where a.c_fullname = substr(topNode,1,instr(topNode,'\',1,3));
        exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
        
        stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Update visual attributes for study nodes in I2B2METADATA i2b2',rowCt,stepCt,'Done');
    
	

  
  --Build concept Counts
  --Also marks any i2B2 records with no underlying data as Hidden, need to do at Trial level because there may be multiple platform and there is no longer
  -- a unique top-level node for miRNA data
  
        perform i2b2_create_concept_counts(topNode ,jobID );
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Create concept counts',0,stepCt,'Done');
	
	--	delete each node that is hidden
	begin
	 FOR r_delNodes in delNodes Loop

    --	deletes hidden nodes for a trial one at a time
		
		perform i2b2_delete_1_node(r_delNodes.c_fullname);
		stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
		tText := 'Deleted node: ' || r_delNodes.c_fullname;
		get diagnostics rowCt := ROW_COUNT;
		perform cz_write_audit(jobId,databaseName,procedureName,tText,rowCt,stepCt,'Done');

	END LOOP;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;  	


  --Reload Security: Inserts one record for every I2B2 record into the security table
	begin
		perform i2b2_load_security_data(jobId);
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Load security data',0,stepCt,'Done');

--	tag data with probeset_id from reference.probeset_deapp

	begin
	execute ('truncate table tm_wz.WT_SUBJECT_MIRNA_PROBESET');
	
	--	note: assay_id represents a unique subject/site/sample

	
	insert into WT_SUBJECT_MIRNA_PROBESET  --mod
	(probeset_id
--	,expr_id
	,intensity_value
	,patient_id
--	,sample_cd
--	,subject_id
	,trial_name
	,assay_id
	)
	select    p.probeset_id 
		  ,avg(md.intensity_value::numeric)
                  ,sd.patient_id
		  ,TrialId
		  ,sd.assay_id
	from LT_SRC_QPCR_MIRNA_DATA md left outer join deapp.de_subject_sample_mapping sd on sd.sample_cd = md.expr_id
                ,mirna_probeset_deapp p
	where sd.platform = mirna_type
	  and sd.trial_name =TrialId
	  and sd.source_cd = sourceCd
	 -- and sd.gpl_id = gs.id_ref
	  and md.probeset =p.probeset-- gs.mirna_id
	  and CASE WHEN dataType ='R' THEN sign(md.intensity_value::numeric) ELSE 1 END <> -1  ---UAT 163 changes done,UAT 154 changes on 19/03/2014
	  and sd.subject_id in (select subject_id from lt_src_mirna_subj_samp_map) 
    and sd.gpl_id = p.platform
	group by  p.probeset_id 
        ,sd.patient_id,sd.assay_id;
        exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);
		perform tm_cz.cz_end_audit (jobID, 'FAIL');
		return -16;
	end;
		  
	pExists := rowCt;
	
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'Insert into DEAPP wt_subject_mirna_probeset',rowCt,stepCt,'Done');
	
			
	if pExists = 0 then
		perform cz_write_audit(jobId,databasename,procedurename,'No probeset records',1,stepCt,'ERROR');
		perform cz_error_handler(jobid,procedurename, '-1', 'Application raised error');
		perform cz_end_audit (jobId,'FAIL');
		return 165;
	end if;

	--	insert into de_subject_mirna_data when dataType is T (transformed)
 
	if dataType = 'T' then
		begin
		insert into de_subject_mirna_data
		(trial_source
		,probeset_id
		,assay_id
		,patient_id
		--,sample_id
		--,subject_id
		,trial_name
		,zscore
		)
		select (TrialId || ':' || sourceCd)
			  ,probeset_id
			  ,assay_id
			  ,patient_id
			  --,sample_id
			  --,subject_id
			  ,trial_name
			  /*,case when intensity_value < -2.5
			        then -2.5
					when intensity_value > 2.5
					then 2.5
					else intensity_value
			   end as zscore */
                           ,intensity_value as zscore
		from WT_SUBJECT_MIRNA_PROBESET --mod
		where trial_name = TrialID    ;
		stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
		perform cz_write_audit(jobId,databaseName,procedureName,'Insert transformed into DEAPP de_subject_mirna_data',rowCt,stepCt,'Done');
		exception
		when others then
			errorNumber := SQLSTATE;
			errorMessage := SQLERRM;
			perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);
			perform tm_cz.cz_end_audit (jobID, 'FAIL');
			return -16;
		end;	
	else
		
	--	Calculate ZScores and insert data into de_subject_mirna_data.  The 'L' parameter indicates that the gene expression data will be selected from
	--	wt_subject_mirna_probeset as part of a Load.  

		if dataType = 'R' or dataType = 'L' then
			begin
			 if mirna_type='MIRNA_QPCR' then
			perform i2b2_mirna_zscore_calc(TrialID,'L',jobId,'R',logBase,sourceCD);----donot do log transform
                        else
                        perform i2b2_mirna_zscore_calc(TrialID,'L',jobId,'L',logBase,sourceCD);----do log transform
                        end if;
			stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
			perform cz_write_audit(jobId,databaseName,procedureName,'Calculate Z-Score',0,stepCt,'Done');
			exception
			when others then
				errorNumber := SQLSTATE;
				errorMessage := SQLERRM;
				perform tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage);	
				perform tm_cz.cz_end_audit (jobID, 'FAIL');
				return -16;
			end;
		end if;
	
	end if;

    ---Cleanup OVERALL JOB if this proc is being run standalone
	
	stepCt := stepCt + 1; get diagnostics rowCt := ROW_COUNT;
	perform cz_write_audit(jobId,databaseName,procedureName,'End i2b2_process_QPCR_miRNA_DATA',0,stepCt,'Done');

	IF newJobFlag = 1
	THEN
		perform cz_end_audit (jobID, 'SUCCESS');
	END IF;
	
	return 0;
	
	EXCEPTION
	WHEN OTHERS THEN
		--Handle errors.
		perform cz_error_handler (jobID, procedureName, '-1', 'Application raised error');
		--End Proc
		perform cz_end_audit (jobID, 'FAIL');
		return 16;
END;
$$;

