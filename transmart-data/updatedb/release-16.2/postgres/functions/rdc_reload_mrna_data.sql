--
-- Name: rdc_reload_mrna_data(text, text, text, bigint, bigint); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE OR REPLACE FUNCTION tm_cz.rdc_reload_mrna_data(trial_id text, data_type text DEFAULT 'R'::text, source_cd text DEFAULT 'STD'::text, log_base bigint DEFAULT 2, currentjobid bigint DEFAULT NULL::bigint) RETURNS void
    LANGUAGE plpgsql
    AS $_$
DECLARE


	--	JEA@20111202	One-off to re-zscore gene expression data for a study

  TrialID		varchar(100);
  RootNode		varchar(2000);
  root_level	integer;
  topNode		varchar(2000);
  topLevel		integer;
  tPath			varchar(2000);
  study_name	varchar(100);
  sourceCd		varchar(50);

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
  
    --Audit variables
  newJobFlag integer(1);
  databaseName varchar(100);
  procedureName varchar(100);
  jobID bigint;
  stepCt bigint;
  
  --unmapped_patients exception;
  missing_platform	exception;
  missing_tissue	EXCEPTION;
  unmapped_platform exception;
  


BEGIN
	TrialID := upper(trial_id);
--	topNode := REGEXP_REPLACE('\' || top_node || '\','(\\){2,}', '\');	
--	select length(topNode)-length(replace(topNode,'\','')) into topLevel from dual;
	
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
	sourceCd := upper(coalesce(source_cd,'STD'));

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
	cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_process_mrna_data',0,stepCt,'Done');
		
	--	truncate tmp tables

	EXECUTE('truncate table tm_wz.wt_subject_microarray_logs');
	EXECUTE('truncate table tm_wz.wt_subject_microarray_calcs');
	EXECUTE('truncate table tm_wz.wt_subject_microarray_med');
	
	select count(*) 
	into idxExists
	from all_indexes
	where table_name = 'WT_SUBJECT_MICROARRAY_LOGS'
	  and index_name = 'WT_SUBJECT_MRNA_LOGS_I1'
	  and owner = 'TM_WZ';
		
	if idxExists = 1 then
		EXECUTE('drop index tm_wz.wt_subject_mrna_logs_i1');		
	end if;
	
	select count(*) 
	into idxExists
	from all_indexes
	where table_name = 'WT_SUBJECT_MICROARRAY_CALCS'
	  and index_name = 'WT_SUBJECT_MRNA_CALCS_I1'
	  and owner = 'TM_WZ';
		
	if idxExists = 1 then
		EXECUTE('drop index tm_wz.wt_subject_mrna_calcs_i1');
	end if;
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Truncate work tables in TM_WZ',0,stepCt,'Done');
	
	insert into wt_subject_microarray_logs 
	(probeset_id
	,intensity_value
	,assay_id
	,log_intensity
	,patient_id
	,sample_id
	,subject_id
	)
	PERFORM distinct gs.probeset_id
		  ,avg(md.intensity_value)
		  ,sd.assay_id
		  ,avg(md.intensity_value)
		  ,sd.patient_id
		  ,sd.sample_id
		  ,sd.subject_id
	from de_subject_sample_mapping sd
		,lz_src_mrna_data md   
		,de_mrna_annotation gs
	where sd.sample_cd = md.expr_id
	  and sd.platform = 'MRNA_AFFYMETRIX'
	  and sd.trial_name = TrialId
	  and md.trial_name = TrialId
	  and sd.gpl_id = gs.gpl_id
	  and md.probeset = gs.probe_id
	  group by gs.probeset_id
		  ,sd.assay_id
		  ,sd.patient_id
		  ,sd.sample_id
		  ,sd.subject_id;
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Loaded data for trial in TM_WZ wt_subject_microarray_logs',SQL%ROWCOUNT,stepCt,'Done');

	commit;
    
	EXECUTE('create index tm_wz.wt_subject_mrna_logs_i1 on tm_wz.wt_subject_microarray_logs (trial_name, probeset_id) nologging  tablespace "INDX"');
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ wt_subject_microarray_logs',0,stepCt,'Done');
		
--	calculate mean_intensity, median_intensity, and stddev_intensity per experiment, probe

	insert into wt_subject_microarray_calcs
	(trial_name
	,probeset_id
	,mean_intensity
	,median_intensity
	,stddev_intensity
	)
	PERFORM d.trial_name 
		  ,d.probeset_id
		  ,avg(log_intensity)
		  ,median(log_intensity)
		  ,stddev(log_intensity)
	from wt_subject_microarray_logs d 
	group by d.trial_name 
			,d.probeset_id;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Calculate intensities for trial in TM_WZ wt_subject_microarray_calcs',SQL%ROWCOUNT,stepCt,'Done');

	commit;

	EXECUTE('create index tm_wz.wt_subject_mrna_calcs_i1 on tm_wz.wt_subject_microarray_calcs (trial_name, probeset_id) nologging tablespace "INDX"');
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ wt_subject_microarray_calcs',0,stepCt,'Done');
		
-- calculate zscore

	insert into wt_subject_microarray_med parallel 
	(probeset_id
	,intensity_value
	,log_intensity
	,assay_id
	,mean_intensity
	,stddev_intensity
	,median_intensity
	,zscore
	,patient_id
	,sample_id
	,subject_id)
	PERFORM d.probeset_id
		  ,d.intensity_value 
		  ,d.log_intensity 
		  ,d.assay_id  
		  ,c.mean_intensity 
		  ,c.stddev_intensity 
		  ,c.median_intensity 
		  ,CASE WHEN stddev_intensity=0 THEN 0 ELSE (log_intensity - median_intensity ) / stddev_intensity END
		  ,d.patient_id
		  ,d.sample_id
		  ,d.subject_id
    from wt_subject_microarray_logs d 
		,wt_subject_microarray_calcs c 
    where d.probeset_id = c.probeset_id;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Calculate Z-Score for trial in TM_WZ wt_subject_microarray_med',SQL%ROWCOUNT,stepCt,'Done');

    commit;

	select count(*) 
	into idxExists
	from all_indexes
	where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
	  and index_name = 'MRNA_INDX1'
	  and owner = 'DEAPP';
		
	if idxExists = 0 then
		EXECUTE('create index deapp.mrna_idx1 on deapp.de_subject_microarray_data (trial_name) nologging tablespace "INDX"');
	end if;
	
	delete from de_subject_microarray_data
	where trial_name = TrialId;
	
	EXECUTE('drop index deapp.mrna_idx1');

	insert into de_subject_microarray_data
	(trial_name
	,assay_id
	,probeset_id
	,raw_intensity 
	,log_intensity
	,zscore
	,patient_id
	,sample_id
	,subject_id
	)
	PERFORM TrialId
	      ,m.assay_id
	      ,m.probeset_id 
		  ,case when dataType = 'R' then m.intensity_value
		        when dataType = 'L' then case when logBase = -1 then null else power(logBase, m.log_intensity) end
				else null end
		  ,m.log_intensity
	      ,round(CASE WHEN m.zscore < -2.5 THEN -2.5 WHEN m.zscore >  2.5 THEN  2.5 ELSE round(m.zscore,5) END,5)
		  ,m.patient_id
		  ,m.sample_id
		  ,m.subject_id
	from wt_subject_microarray_med m;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Insert data for trial in DEAPP de_subject_microarray_data',SQL%ROWCOUNT,stepCt,'Done');

  	commit;
	
--	cleanup tmp_ files

	EXECUTE('truncate table tm_wz.wt_subject_microarray_logs');
	EXECUTE('truncate table tm_wz.wt_subject_microarray_calcs');
	EXECUTE('truncate table tm_wz.wt_subject_microarray_med');

   	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Truncate work tables in TM_WZ',0,stepCt,'Done');
	
    ---Cleanup OVERALL JOB if this proc is being run standalone
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'End i2b2_process_mrna_data',0,stepCt,'Done');

	IF newJobFlag = 1
	THEN
		cz_end_audit (jobID, 'SUCCESS');
	END IF;
	
	--select 0 into rtn_code from dual;

	EXCEPTION
	--when unmapped_patients then
	--	cz_write_audit(jobId,databasename,procedurename,'No site_id/subject_id mapped to patient_dimension',1,stepCt,'ERROR');
	--	cz_error_handler(jobId, procedureName, SQLSTATE, SQLERRM);
	--	cz_end_audit (jobId,'FAIL');
	when missing_platform then
		cz_write_audit(jobId,databasename,procedurename,'Platform data missing from one or more subject_sample mapping records',1,stepCt,'ERROR');
		cz_error_handler(jobId, procedureName, SQLSTATE, SQLERRM);
		cz_end_audit (jobId,'FAIL');
		--select 16 into rtn_code from dual;
	when missing_tissue then
		cz_write_audit(jobId,databasename,procedurename,'Tissue Type data missing from one or more subject_sample mapping records',1,stepCt,'ERROR');
		cz_error_handler(jobId, procedureName, SQLSTATE, SQLERRM);
		CZ_END_AUDIT (JOBID,'FAIL');
		--select 16 into rtn_code from dual;
	when unmapped_platform then
		cz_write_audit(jobId,databasename,procedurename,'Platform not found in de_mrna_annotation',1,stepCt,'ERROR');
		cz_error_handler(jobId, procedureName, SQLSTATE, SQLERRM);
		cz_end_audit (jobId,'FAIL');
		--select 16 into rtn_code from dual;
	WHEN OTHERS THEN
		--Handle errors.
		cz_error_handler(jobId, procedureName, SQLSTATE, SQLERRM);
		--End Proc
		cz_end_audit (jobID, 'FAIL');
		--select 16 into rtn_code from dual;
END;
 
$_$;

ALTER FUNCTION tm_cz.rdc_reload_mrna_data(text, text, text, bigint, bigint)
    OWNER TO tm_cz;

GRANT EXECUTE ON FUNCTION tm_cz.rdc_reload_mrna_data(text, text, text, bigint, bigint) TO tm_cz;