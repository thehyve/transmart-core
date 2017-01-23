--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_RNA_SEQ_ZSCORE_CALC
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_RNA_SEQ_ZSCORE_CALC" 
(
  trial_id VARCHAR2
 ,run_type varchar2 := 'L'
 ,currentJobID NUMBER := null
 ,data_type varchar2 := 'R'
 ,log_base	number := 2
 ,source_cd	varchar2
)
AS
/*************************************************************************
* This stored procedure is used to calculate z-scores for RNA sequencing data load
* Date:10/23/2013
******************************************************************/

  TrialID varchar2(50);
  sourceCD	varchar2(50);
  sqlText varchar2(2000);
  runType varchar2(10);
  dataType varchar2(10);
  stgTrial varchar2(50);
  idxExists number;
  pExists	number;
  nbrRecs number;
  logBase number;
   
  --Audit variables
  newJobFlag INTEGER(1);
  databaseName VARCHAR(100);
  procedureName VARCHAR(100);
  jobID number(18,0);
  stepCt number(18,0);
  
  --  exceptions
  invalid_runType exception;
  trial_mismatch exception;
  trial_missing exception;
  
BEGIN

	TrialId := trial_id;
	runType := run_type;
	dataType := data_type;
	logBase := log_base;
	sourceCd := source_cd;
	  
  --Set Audit Parameters
  newJobFlag := 0; -- False (Default)
  jobID := currentJobID;

  SELECT sys_context('USERENV', 'CURRENT_SCHEMA') INTO databaseName FROM dual;
  procedureName := $$PLSQL_UNIT;

  --Audit JOB Initialization
  --If Job ID does not exist, then this is a single procedure run and we need to create it
  IF(jobID IS NULL or jobID < 1)
  THEN
    newJobFlag := 1; -- True
    cz_start_audit (procedureName, databaseName, jobID);
  END IF;
    	
  stepCt := 0;
  
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Starting zscore calc for ' || TrialId || ' RunType: ' || runType || ' dataType: ' || dataType,0,stepCt,'Done');
  
	if runType != 'L' then
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Invalid runType passed - procedure exiting',SQL%ROWCOUNT,stepCt,'Done');
		raise invalid_runType;
	end if;
  
--	For Load, make sure that the TrialId passed as parameter is the same as the trial in stg_subject_RNA_sequencing_data
--	If not, raise exception

	if runType = 'L' then
		select distinct trial_name into stgTrial
		from wt_subject_rna_probeset;
		
		if stgTrial != TrialId then
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'TrialId not the same as trial in wt_subject_rna_probeset - procedure exiting',SQL%ROWCOUNT,stepCt,'Done');
			raise trial_mismatch;
		end if;
	end if;
   
/*	remove Reload processing
--	For Reload, make sure that the TrialId passed as parameter has data in de_subject_RNA_data
--	If not, raise exception

	if runType = 'R' then
		select count(*) into idxExists
		from de_subject_RNA_data
		where trial_name = TrialId;
		
		if idxExists = 0 then
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'No data for TrialId in de_subject_RNA_data - procedure exiting',SQL%ROWCOUNT,stepCt,'Done');
			raise trial_missing;
		end if;
	end if;
*/
   
--	truncate tmp tables

	execute immediate('truncate table tm_wz.wt_subject_rna_logs');
	execute immediate('truncate table tm_wz.wt_subject_rna_calcs');
	execute immediate('truncate table tm_wz.wt_subject_rna_med');
	
	select count(*) 
	into idxExists 
	from all_indexes
	where table_name = 'WT_SUBJECT_RNA_LOGS'
	  and index_name = 'WT_SUBJECT_RNA_LOGS_I1'
	  and owner = 'TM_WZ';
		
	if idxExists = 1 then
		execute immediate('drop index tm_wz.WT_SUBJECT_RNA_LOGS_I1');		
	end if;
	
	select count(*) 
	into idxExists
	from all_indexes
	where table_name = 'WT_SUBJECT_RNA_CALCS'
	  and index_name = 'WT_SUBJECT_RNA_CALCS_I1'
	  and owner = 'TM_WZ';
		
	if idxExists = 1 then
		execute immediate('drop index tm_wz.WT_SUBJECT_RNA_CALCS_I1');
	end if;


	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Truncate work tables in TM_WZ',0,stepCt,'Done');
	
	--	if dataType = L, use intensity_value as log_intensity
	--	if dataType = R, always use intensity_value


	if dataType = 'L' then
/*	Remove Reload processing
		if runType = 'R' then
			insert into wt_subject_microarray_logs 
			(probeset_id
			,intensity_value
			,assay_id
			,log_intensity
			,patient_id
			,sample_id
			,subject_id
			)
			select probeset_id
				  ,raw_intensity 
				  ,assay_id
				  ,log_intensity
				  ,patient_id
				  ,sample_id
				  ,subject_id
			from de_subject_microarray_data  
			where trial_name =  TrialID;
		else
*/
			insert into wt_subject_rna_logs 
			(probeset_id
			,intensity_value
			,assay_id
			,log_intensity
			,patient_id
		--	,sample_cd
		--	,subject_id
			)
			select probeset_id
				  ,intensity_value  
				  ,assay_id 
				  ,intensity_value
				  ,patient_id
			--	  ,sample_cd
			--	  ,subject_id
			from wt_subject_rna_probeset
			where trial_name = TrialId;
		--end if;
	else
	/*	remove Reload processing
		if runType = 'R' then
			insert into wt_subject_rna_logs 
			(probeset_id
			,intensity_value
			,assay_id
			,log_intensity
			,patient_id
			,sample_id
			,subject_id
			)
			select probeset_id
				  ,raw_intensity 
				  ,assay_id  
				  ,log(2,raw_intensity)
				  ,patient_id
				  ,sample_id
				  ,subject_id
			from de_subject_rna_data 
			where trial_name =  TrialID;
		else
*/
			insert into wt_subject_rna_logs 
			(probeset_id
			,intensity_value
			,assay_id
			,log_intensity
			,patient_id
		--	,sample_cd
		--	,subject_id
			)
			select probeset_id
				  ,intensity_value 
				  ,assay_id 
				  --,CASE WHEN intensity_value<=0 THEN log(2,(intensity_value+0.001)) ELSE log(2,intensity_value) END
                                  ,log(2,(intensity_value+0.001))    ---UAT 154 changes on 19/03/2014
				  ,patient_id
		--		  ,sample_cd
		--		  ,subject_id
			from wt_subject_rna_probeset
			where trial_name = TrialId;
--		end if;
	end if;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Loaded data for trial in TM_WZ wt_subject_rna_logs',SQL%ROWCOUNT,stepCt,'Done');

	commit;
    
	execute immediate('create index tm_wz.WT_SUBJECT_RNA_LOGS_I1 on tm_wz.wt_subject_rna_logs (trial_name, probeset_id) nologging  tablespace "INDX"');
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ WT_SUBJECT_RNA_LOGS_I1',0,stepCt,'Done');
		
--	calculate mean_intensity, median_intensity, and stddev_intensity per experiment, probe

	insert into wt_subject_rna_calcs
	(trial_name
	,probeset_id
	,mean_intensity
	,median_intensity
	,stddev_intensity
	)
	select d.trial_name 
		  ,d.probeset_id
		  ,avg(log_intensity)
		  ,median(log_intensity)
		  ,stddev(log_intensity)
	from wt_subject_rna_logs d 
	group by d.trial_name 
			,d.probeset_id;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Calculate intensities for trial in TM_WZ wt_subject_rna_calcs',SQL%ROWCOUNT,stepCt,'Done');

	commit;

	execute immediate('create index tm_wz.WT_SUBJECT_RNA_CALCS_I1 on tm_wz.wt_subject_rna_calcs (trial_name, probeset_id) nologging tablespace "INDX"');
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ WT_SUBJECT_RNA_CALCS_I1',0,stepCt,'Done');
		
-- calculate zscore

	insert into wt_subject_rna_med parallel 
	(probeset_id
	,intensity_value
	,log_intensity
	,assay_id
	,mean_intensity
	,stddev_intensity
	,median_intensity
	,zscore
	,patient_id
--	,sample_cd
--	,subject_id
	)
	select d.probeset_id
		  ,d.intensity_value 
		  ,d.log_intensity 
		  ,d.assay_id  
		  ,c.mean_intensity 
		  ,c.stddev_intensity 
		  ,c.median_intensity 
		  ,CASE WHEN stddev_intensity=0 THEN 0 ELSE (log_intensity - median_intensity ) / stddev_intensity END
		  ,d.patient_id
	--	  ,d.sample_cd
	--	  ,d.subject_id
    from wt_subject_rna_logs d 
		,wt_subject_rna_calcs c 
    where d.probeset_id = c.probeset_id;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Calculate Z-Score for trial in TM_WZ wt_subject_rna_med',SQL%ROWCOUNT,stepCt,'Done');

    commit;
  
/*
	select count(*) into nbrRecs
	from wt_subject_rna_med;
	
	if nbrRecs > 10000000 then
		i2b2_RNA_sequencing_index_maint('DROP',,jobId);
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Drop indexes on DEAPP de_subject_rna_data',0,stepCt,'Done');
	else
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Less than 10M records, index drop bypassed',0,stepCt,'Done');
	end if;
*/	


	insert into de_subject_rna_data
	(trial_source
	,trial_name
	,assay_id
	,probeset_id
	,raw_intensity 
	,log_intensity
	,zscore
	,patient_id
	--,sample_id
	--,subject_id
	)
	select TrialId || ':' || sourceCD
		  ,TrialId
	      ,m.assay_id
	      ,m.probeset_id 
		  ,case when dataType = 'R' then m.intensity_value
				when dataType = 'L' 
				then case when logBase = -1 then null else power(logBase, m.log_intensity) end
				else null
				end as raw_intensity
	    --  ,decode(dataType,'R',m.intensity_value,'L',power(logBase, m.log_intensity),null)
		  ,round(m.log_intensity,4)
	      ,CASE WHEN m.zscore < -2.5 THEN -2.5 WHEN m.zscore >  2.5 THEN  2.5 ELSE round(m.zscore,5) END
		  ,m.patient_id
	--	  ,m.sample_id
	--	  ,m.subject_id
	from wt_subject_rna_med m;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Insert data for trial in DEAPP de_subject_rna_data',SQL%ROWCOUNT,stepCt,'Done');

  	commit;

--	add indexes, if indexes were not dropped, procedure will not try and recreate
/*
	i2b2_RNA_sequencing_index_maint('ADD',,jobId);
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Add indexes on DEAPP de_subject_rna_data',0,stepCt,'Done');
*/
	
--	cleanup tmp_ files

	execute immediate('truncate table tm_wz.wt_subject_rna_logs');
	execute immediate('truncate table tm_wz.wt_subject_rna_calcs');
	execute immediate('truncate table tm_wz.wt_subject_rna_med');

   	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Truncate work tables in TM_WZ',0,stepCt,'Done');
    
    ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1
  THEN
    cz_end_audit (jobID, 'SUCCESS');
  END IF;

  EXCEPTION

  WHEN invalid_runType or trial_mismatch or trial_missing then
    --Handle errors.
    cz_error_handler (jobID, procedureName);
    --End Proc
    cz_end_audit (jobID, 'FAIL');
  when OTHERS THEN
    --Handle errors.
    cz_error_handler (jobID, procedureName);
    --End Proc
    cz_end_audit (jobID, 'FAIL');
	
END;
/
 
