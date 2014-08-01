--
-- Name: i2b2_metabolomics_zscore_calc(character varying, character varying, character varying, numeric, character varying, numeric); Type: FUNCTION; Schema: biomart_user; Owner: -
--
CREATE FUNCTION i2b2_metabolomics_zscore_calc(trial_id character varying, source_cd character varying, run_type character varying DEFAULT 'L'::character varying, currentjobid numeric DEFAULT (-1), data_type character varying DEFAULT 'R'::character varying, log_base numeric DEFAULT 2) RETURNS void
    LANGUAGE plpgsql
    AS $_$
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
  idxExists integer;
  pExists	integer;
  nbrRecs integer;
  logBase numeric;
   
  --Audit variables
  newJobFlag integer(1);
  databaseName varchar(100);
  procedureName varchar(100);
  jobID numeric;
  stepCt integer;
  
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
	  RAISE NOTICE 'DK0';
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
	cz_write_audit(jobId,databaseName,procedureName,'Starting zscore calc for ' || TrialId || ' RunType: ' || runType || ' dataType: ' || dataType,0,stepCt,'Done');
  
	if runType != 'L' then
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Invalid runType passed - procedure exiting'
,SQL%ROWCOUNT,stepCt,'Done');
		raise invalid_runType;
	end if;
  
--	For Load, make sure that the TrialId passed as parameter is the same as the trial in stg_subject_METABOLOMICS_data
--	If not, raise exception

	if runType = 'L' then
		select distinct trial_name into stgTrial
		from WT_SUBJECT_MBOLOMICS_PROBESET;
		
		if stgTrial != TrialId then
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'TrialId not the same as trial in WT_SUBJECT_MBOLOMICS_PROBESET - procedure exiting'
,SQL%ROWCOUNT,stepCt,'Done');
			--raise trial_mismatch;
		end if;
	end if;

/*	remove Reload processing
--	For Reload, make sure that the TrialId passed as parameter has data in de_subject_METABOLOMICS_data
--	If not, raise exception

	if runType = 'R' then
		select count(*) into idxExists
		from DE_SUBJECT_METABOLOMICS_DATA
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
  
		where trial_name = TrialId;
		
		if idxExists = 0 then
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'No data for TrialId in DE_SUBJECT_METABOLOMICS_DATA - procedure exiting',SQL%ROWCOUNT,stepCt,'Done');
			raise trial_missing;
		end if;
	end if;
*/
   
--	truncate tmp tables

	EXECUTE('truncate table tm_wz.WT_SUBJECT_METABOLOMICS_LOGS');
	EXECUTE('truncate table tm_wz.WT_SUBJECT_METABOLOMICS_CALCS');
	EXECUTE('truncate table tm_wz.WT_SUBJECT_METABOLOMICS_MED');

	select count(*) 
	into idxExists
	from all_indexes
	where table_name = 'WT_SUBJECT_METABOLOMICS_LOGS'
	  and index_name = 'WT_SUBJECT_MBOLOMICS_LOGS_I1'
	  and owner = 'TM_WZ';

	if idxExists = 1 then
		EXECUTE('drop index tm_wz.WT_SUBJECT_MBOLOMICS_LOGS_I1');		
	end if;
	
	select count(*) 
	into idxExists
	from all_indexes
	where table_name = 'WT_SUBJECT_METABOLOMICS_CALCS'
	  and index_name = 'WT_SUBJECT_METABOLOMICS_CALCS_I1'
	  and owner = 'TM_WZ';

	if idxExists = 1 then
		EXECUTE('drop index tm_wz.WT_SUBJECT_METABOLOMICS_CALCS_I1');
	end if;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Truncate work tables in TM_WZ',0,stepCt,'Done');
	
	--	if dataType = L, use intensity_value as log_intensity
	--	if dataType = R, always use intensity_value


	if dataType = 'L' then

		insert into WT_SUBJECT_METABOLOMICS_LOGS 
			(probeset
			,intensity_value
			,assay_id
			,log_intensity
			,patient_id
		--	,sample_cd
			,subject_id
			)
			PERFORM probeset
				  ,intensity_value  
				  ,assay_id 
				  ,intensity_value
				  ,patient_id
			--	  ,sample_cd
				  ,subject_id
			from WT_SUBJECT_MBOLOMICS_PROBESET
			where trial_name = TrialId;
           
		--end if;
	else	

                	insert into WT_SUBJECT_METABOLOMICS_LOGS 
			(probeset
			,intensity_value
			,assay_id
			,log_intensity
			,patient_id
		--	,sample_cd
			,subject_id
			)
			PERFORM probeset
				  ,intensity_value 
				  ,assay_id 
				  ,log(2,intensity_value)
				  ,patient_id
		--		  ,sample_cd
				  ,subject_id
			from WT_SUBJECT_MBOLOMICS_PROBESET
			where trial_name = TrialId;
--		end if;

	end if;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Loaded data for trial in TM_WZ wt_subject_mirna_logs',SQL%ROWCOUNT,stepCt,'Done');

	commit;

	EXECUTE('create index tm_wz.WT_SUBJECT_MBOLOMICS_LOGS_I1 on tm_wz.WT_SUBJECT_METABOLOMICS_LOGS (trial_name, probeset) nologging  tablespace "INDX"');
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ WT_SUBJECT_MBOLOMICS_LOGS_I1',0,stepCt,'Done');
		
--	calculate mean_intensity, median_intensity, and stddev_intensity per experiment, probe

	insert into WT_SUBJECT_METABOLOMICS_CALCS
	(trial_name
	,probeset
	,mean_intensity
	,median_intensity
	,stddev_intensity
	)
	PERFORM d.trial_name 
		  ,d.probeset
		  ,avg(log_intensity)
		  ,median(log_intensity)
		  ,stddev(log_intensity)
	from WT_SUBJECT_METABOLOMICS_LOGS d 
	group by d.trial_name 
			,d.probeset;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Calculate intensities for trial in TM_WZ WT_SUBJECT_METABOLOMICS_CALCS',SQL%ROWCOUNT,stepCt,'Done');

	commit;

	--execute immediate('create index tm_wz.wt_subject_METABOLOMICS_calcs_i1 on tm_wz.WT_SUBJECT_METABOLOMICS_CALCS (trial_name, probeset_id) nologging tablespace "INDX"');
	--stepCt := stepCt + 1;
	--cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ WT_SUBJECT_METABOLOMICS_CALCS',0,stepCt,'Done');
		
-- calculate zscore

	
        insert into WT_SUBJECT_METABOLOMICS_MED parallel 
	(probeset
	,intensity_value
	,log_intensity
	,assay_id
	,mean_intensity
	,stddev_intensity
	,median_intensity
	,zscore
	,patient_id
--	,sample_cd
	,subject_id
	)
	PERFORM d.probeset
		  ,d.intensity_value 
		  ,d.log_intensity 
		  ,d.assay_id  
		  ,c.mean_intensity 
		  ,c.stddev_intensity 
		  ,c.median_intensity 
		  ,(CASE WHEN stddev_intensity=0 THEN 0 ELSE (log_intensity - median_intensity ) / stddev_intensity END)
		  ,d.patient_id
	--	  ,d.sample_cd
		  ,d.subject_id
    from WT_SUBJECT_METABOLOMICS_LOGS d 
		,WT_SUBJECT_METABOLOMICS_CALCS c 
    where trim(d.probeset) = c.probeset;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Calculate Z-Score for trial in TM_WZ WT_SUBJECT_METABOLOMICS_MED',SQL%ROWCOUNT,stepCt,'Done');

    commit;

/*
	select count(*) into n
	select count(*) into nbrRecs
	from WT_SUBJECT_METABOLOMICS_MED;
	
	if nbrRecs > 10000000 then
		i2b2_mrna_index_maint('DROP',,jobId);
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Drop indexes on DEAPP de_subject_METABOLOMICS_data',0,stepCt,'Done');
	else
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Less than 10M records, index drop bypassed',0,stepCt,'Done');
	end if;
*/
	


/*
	insert into DE_SUBJECT_METABOLOMICS_DATA
	(
        trial_source
        ,trial_name
	,metabolite_annotation_id
	--,component
	--,gene_symbol
	--,gene_id
	,assay_id
	,subject_id
	,raw_intensity 
        ,log_intensity
	,zscore
	,patient_id
		)
                select m.trial_name || ':' || mpp.source_cd,
                  TrialID
                ,d.Id
		  ,m.assay_id
                  ,m.subject_id 
                  ,m.intensity_value
                  ,log(2,m.intensity_value)
			  ,case when m.intensity_value < -2.5
			        then -2.5
					when m.intensity_value > 2.5
					then 2.5
					else m.intensity_value
			   end as zscore
                           ,m.patient_id
                from WT_SUBJECT_MBOLOMICS_PROBESET  m,
                (select distinct mp.source_cd From "TM_LZ"."LT_SRC_METABOLOMIC_MAP" mp where rownum = 1 and mp.trial_name =TrialID) mpp
                ,DEAPP.DE_METABOLITE_ANNOTATION d
		where m.trial_name = TrialID
                and d.biochemical_name = m.probeset;
        */
        insert into DE_SUBJECT_METABOLOMICS_DATA
	(
        trial_source
	,trial_name
	,metabolite_annotation_id
	--,component
	--,gene_symbol
	--,gene_id
	,assay_id
	,subject_id
	,raw_intensity 
        ,log_intensity
	,zscore
	,patient_id
	)
	PERFORM 
                  TrialId || ':' || mpp.source_cd,
                  TrialId
                 ,d.id
                 --,m.probeset_id 
                 --,d.hmdb_id
                 --,d.biomarker_id
                 ,m.assay_id
                 ,m.subject_id
	    --  ,decode(dataType,'R',m.intensity_value,'L',power(logBase, m.log_intensity),null)
                  ,m.intensity_value
		  ,round(m.log_intensity,4)
                  ,round(CASE WHEN m.zscore < -2.5 THEN -2.5 WHEN m.zscore >  2.5 THEN  2.5 ELSE round(m.zscore,5) END,5)		  
                   ,m.patient_id
	from WT_SUBJECT_METABOLOMICS_MED m,
        (select distinct mp.source_cd,mp.platform From "TM_LZ"."LT_SRC_METABOLOMIC_MAP" mp LIMIT 1 OFFSET 1 and mp.trial_name =TrialID) mpp                
       , DE_METABOLITE_ANNOTATION d
        where trim(d.biochemical_name) = trim(m.probeset)
        and d.gpl_id = mpp.platform;
        
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Insert data for trial in DEAPP DE_SUBJECT_METABOLOMICS_DATA',SQL%ROWCOUNT,stepCt,'Done');

  	commit;

--	add indexes, if indexes were not dropped, procedure will not try and recreate
/*
	i2b2_mrna_index_maint('ADD',,jobId);
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Add indexes on DEAPP de_subject_METABOLOMICS_data',0,stepCt,'Done');
*/
	
--	cleanup tmp_ files

	--execute immediate('truncate table tm_wz.WT_SUBJECT_METABOLOMICS_LOGS');
	--execute immediate('truncate table tm_wz.WT_SUBJECT_METABOLOMICS_CALCS');
	--execute immediate('truncate table tm_wz.WT_SUBJECT_METABOLOMICS_MED');

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


    cz_end_audit (jobID, 'FAIL');
	
END;
 
$_$;

