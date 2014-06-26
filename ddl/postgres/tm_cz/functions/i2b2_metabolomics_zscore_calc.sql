-- Function: i2b2_metabolomics_zscore_calc(character varying, character varying, character varying, numeric, character varying, numeric)

-- DROP FUNCTION i2b2_metabolomics_zscore_calc(character varying, character varying, character varying, numeric, character varying, numeric);

CREATE OR REPLACE FUNCTION i2b2_metabolomics_zscore_calc(trial_id character varying, partition_name character varying, partition_indx character varying, partitioniD numeric, source_cd character varying, run_type character varying DEFAULT 'L'::character varying, currentjobid numeric DEFAULT (-1), data_type character varying DEFAULT 'R'::character varying, log_base numeric DEFAULT 2)
  RETURNS void AS
$BODY$
DECLARE

/*************************************************************************
This Stored Procedure is used in ETL load METABOLOMICS data
Date:1/3/2014
******************************************************************/

  TrialID varchar(100);
  sourceCD	varchar(50);
  sqlText varchar(2000);
  runType varchar(10);
  dataType varchar(10);
  stgTrial varchar(100);
  idxExists numeric;
  pExists	numeric;
  nbrRecs numeric;
  logBase numeric;
  partitionName varchar(200);
  partitionindx varchar(200);

   
  --Audit variables
  newJobFlag integer;
  databaseName varchar(100);
  procedureName varchar(100);
  jobID numeric;
  stepCt numeric;
  rowCt			bigint;
  rtnCd			integer;
  errorNumber		character varying;
  errorMessage	character varying;

BEGIN

	TrialId := trial_id;
	runType := run_type;
	dataType := data_type;
	logBase := log_base;
	sourceCd := source_cd;
	  RAISE NOTICE 'DK0';
  --Set Audit Parameters
  newJobFlag := 0; -- False (Default)
  jobID := currentJobID;
  
  partitionindx := partition_indx;
  partitionName := partition_name;

  databaseName := 'TM_CZ';
  procedureName := 'i2b2_metabolomics_zscore_calc';

  --Audit JOB Initialization
  --If Job ID does not exist, then this is a single procedure run and we need to create it
  IF(coalesce(jobID::text, '') = '' or jobID < 1)
  THEN
    newJobFlag := 1; -- True
    select cz_start_audit (procedureName, databaseName, jobID) into jobId;
  END IF;
   
  stepCt := 0;
  
	stepCt := stepCt + 1;
	perform cz_write_audit(jobId,databaseName,procedureName,'Starting zscore calc for ' || TrialId || ' RunType: ' || runType || ' dataType: ' || dataType,0,stepCt,'Done');
  
	if runType != 'L' then
		stepCt := stepCt + 1;
		perform cz_write_audit(jobId,databaseName,procedureName,'Invalid runType passed - procedure exiting'
,0,stepCt,'Done');
		select cz_error_handler(jobid,procedurename, '-1', 'Application raised error') into rtnCd;
		select cz_end_audit (jobId,'FAIL') into rtnCd;
		return;
	end if;
  
--	For Load, make sure that the TrialId passed as parameter is the same as the trial in stg_subject_METABOLOMICS_data
--	If not, raise exception

	if runType = 'L' then
		select distinct trial_name into stgTrial
		from WT_SUBJECT_MBOLOMICS_PROBESET;
		
		if stgTrial != TrialId then
			stepCt := stepCt + 1;
			perform cz_write_audit(jobId,databaseName,procedureName,'TrialId not the same as trial in WT_SUBJECT_MBOLOMICS_PROBESET - procedure exiting'
,0,stepCt,'Done');
select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
			select cz_end_audit (jobId,'FAIL') into rtnCd;
			return;
		end if;
	end if;

--	remove Reload processing
--	For Reload, make sure that the TrialId passed as parameter has data in de_subject_METABOLOMICS_data
--	If not, raise exception

	if runType = 'R' then
		select count(*) into idxExists
		from DE_SUBJECT_METABOLOMICS_DATA		
		where trial_name = TrialId;
		
		if idxExists = 0 then
			stepCt := stepCt + 1;
			select cz_write_audit(jobId,databaseName,procedureName,'No data for TrialId in de_subject_rbm_data - procedure exiting'
,0,stepCt,'Done') into rtnCd;
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		select cz_end_audit (jobId,'FAIL') into rtnCd;
		return;
		end if;
	end if;

	EXECUTE('truncate table tm_wz.WT_SUBJECT_METABOLOMICS_LOGS');
	EXECUTE('truncate table tm_wz.WT_SUBJECT_METABOLOMICS_CALCS');
	EXECUTE('truncate table tm_wz.WT_SUBJECT_METABOLOMICS_MED');

	EXECUTE('drop index if exists tm_wz.WT_SUBJECT_MBOLOMICS_LOGS_I1');		
	EXECUTE('drop index if exists tm_wz.WT_SUBJECT_METABOLOMICS_CALCS_I1');

	stepCt := stepCt + 1;
	perform cz_write_audit(jobId,databaseName,procedureName,'Truncate work tables in TM_WZ',0,stepCt,'Done');
	
	--	if dataType = L, use intensity_value as log_intensity
	--	if dataType = R, always use intensity_value
	begin
	if dataType = 'L' then
		insert into WT_SUBJECT_METABOLOMICS_LOGS 
			(probeset
			,intensity_value
			,assay_id
			,log_intensity
			,patient_id
			,subject_id
			)
			select probeset
				  ,intensity_value  
				  ,assay_id 
				  ,intensity_value
				  ,patient_id
				  ,subject_id
			from WT_SUBJECT_MBOLOMICS_PROBESET
			where trial_name = TrialId;
	else	
			insert into WT_SUBJECT_METABOLOMICS_LOGS 
			(probeset
			,intensity_value
			,assay_id
			,log_intensity
			,patient_id
			,subject_id
			)
			select probeset
				  ,intensity_value 
				  ,assay_id 
				  ,CASE WHEN intensity_value <= 0 THEN log(2,(intensity_value + 0.001)) ELSE log(2,intensity_value) END
				  ,patient_id
				  ,subject_id
			from WT_SUBJECT_MBOLOMICS_PROBESET
			where trial_name = TrialId;
	end if;
		get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return;
	end;

	stepCt := stepCt + 1;
	perform cz_write_audit(jobId,databaseName,procedureName,'Loaded data for trial in TM_WZ wt_subject_mirna_logs',rowCt,stepCt,'Done');

	EXECUTE('create index WT_SUBJECT_MBOLOMICS_LOGS_I1 on tm_wz.WT_SUBJECT_METABOLOMICS_LOGS (trial_name, probeset) tablespace "indx"');
	stepCt := stepCt + 1;
	perform cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ WT_SUBJECT_MBOLOMICS_LOGS_I1',0,stepCt,'Done');
		
--	calculate mean_intensity, median_intensity, and stddev_intensity per experiment, probe
	begin
	insert into WT_SUBJECT_METABOLOMICS_CALCS
	(trial_name
	,probeset
	,mean_intensity
	,median_intensity
	,stddev_intensity
	)
	select d.trial_name 
		  ,d.probeset
		  ,avg(log_intensity)
		  ,median(log_intensity)
		  ,coalesce(stddev(log_intensity),0)
	from WT_SUBJECT_METABOLOMICS_LOGS d 
	group by d.trial_name 
			,d.probeset;
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return;
	end;
	stepCt := stepCt + 1;
	perform cz_write_audit(jobId,databaseName,procedureName,'Calculate intensities for trial in TM_WZ WT_SUBJECT_METABOLOMICS_CALCS',rowCt,stepCt,'Done');

-- calculate zscore
	begin
        insert into WT_SUBJECT_METABOLOMICS_MED 
	(probeset
	,intensity_value
	,log_intensity
	,assay_id
	,mean_intensity
	,stddev_intensity
	,median_intensity
	,zscore
	,patient_id
	,subject_id
	)
	select d.probeset
		  ,d.intensity_value 
		  ,d.log_intensity 
		  ,d.assay_id  
		  ,c.mean_intensity 
		  ,c.stddev_intensity 
		  ,c.median_intensity 
		  ,(CASE WHEN stddev_intensity=0 THEN 0 ELSE (log_intensity - median_intensity ) / stddev_intensity END)
		  ,d.patient_id
		  ,d.subject_id
    from WT_SUBJECT_METABOLOMICS_LOGS d 
		,WT_SUBJECT_METABOLOMICS_CALCS c 
    where trim(d.probeset) = c.probeset;
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return;
	end;
	stepCt := stepCt + 1;
	perform cz_write_audit(jobId,databaseName,procedureName,'Calculate Z-Score for trial in TM_WZ WT_SUBJECT_METABOLOMICS_MED',rowCt,stepCt,'Done');
	
    begin
	sqlText := 'insert into ' || partitionName || 
	'(partition_id, trial_source ,trial_name ,metabolite_annotation_id ' ||
	',assay_id ,subject_id ,raw_intensity ,log_intensity ,zscore ,patient_id) ' ||
	'select ' || partitioniD::text || ', ''' || TrialId || '''' ||
		  ',''' || TrialId || ''',d.id ,m.assay_id ,m.subject_id ' ||
           ',m.intensity_value ,round(m.log_intensity,4) ' ||
            ',round(CASE WHEN m.zscore < -2.5 THEN -2.5 WHEN m.zscore >  2.5 THEN  2.5 ELSE round(m.zscore,5) END,5) ' ||  
            ',m.patient_id ' ||
			'from WT_SUBJECT_METABOLOMICS_MED m, ' ||
        '(select distinct mp.source_cd,mp.platform From TM_LZ.LT_SRC_METABOLOMIC_MAP mp where mp.trial_name = ''' || TrialId || ''') as mpp ' ||
		', DE_METABOLITE_ANNOTATION d ' ||
        'where trim(d.biochemical_name) = trim(m.probeset) ' ||
        'and d.gpl_id = mpp.platform ';
        raise notice 'sqlText= %', sqlText;
	execute sqlText;
	get diagnostics rowCt := ROW_COUNT;
	exception
	when others then
		errorNumber := SQLSTATE;
		errorMessage := SQLERRM;
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return;
	end;
	perform cz_write_audit(jobId,databaseName,procedureName,'Insert data for trial in DEAPP DE_SUBJECT_METABOLOMICS_DATA',rowCt,stepCt,'Done');

	sqlText := ' create index ' || partitionIndx || '_idx1 on ' || partitionName || ' using btree (partition_id) tablespace indx';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	sqlText := ' create index ' || partitionIndx || '_idx2 on ' || partitionName || ' using btree (assay_id) tablespace indx';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	sqlText := ' create index ' || partitionIndx || '_idx3 on ' || partitionName || ' using btree (metabolite_annotation_id) tablespace indx';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	sqlText := ' create index ' || partitionIndx || '_idx4 on ' || partitionName || ' using btree (assay_id, metabolite_annotation_id) tablespace indx';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
        
    ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1
  THEN
    perform cz_end_audit (jobID, 'SUCCESS');
  END IF;

END;
 
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION i2b2_metabolomics_zscore_calc(character varying, character varying, character varying, numeric, character varying, numeric)
  OWNER TO tm_cz;
GRANT EXECUTE ON FUNCTION i2b2_metabolomics_zscore_calc(character varying, character varying, character varying, numeric, character varying, numeric) TO tm_cz;
REVOKE ALL ON FUNCTION i2b2_metabolomics_zscore_calc(character varying, character varying, character varying, numeric, character varying, numeric) FROM public;
