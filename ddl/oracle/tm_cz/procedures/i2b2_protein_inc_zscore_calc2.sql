--
-- Type: PROCEDURE; Owner: TM_CZ; Name: I2B2_PROTEIN_INC_ZSCORE_CALC2
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."I2B2_PROTEIN_INC_ZSCORE_CALC2" 
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
This Stored Procedure is used in ETL load PROTEOMICS data
Date:12/9/2013
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
  
--	For Load, make sure that the TrialId passed as parameter is the same as the trial in stg_subject_proteomics_data
--	If not, raise exception

	if runType = 'L' then
		select distinct trial_name into stgTrial
		from WT_SUBJECT_PROTEOMICS_PROBESET;
		
		if stgTrial != TrialId then
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'TrialId not the same as trial in WT_SUBJECT_PROTEOMICS_PROBESET - procedure exiting',SQL%ROWCOUNT,stepCt,'Done');
			raise trial_mismatch;
		end if;
	end if;

/*	remove Reload processing
--	For Reload, make sure that the TrialId passed as parameter has data in DE_SUBJECT_PROTEIN_DATA
--	If not, raise exception

	if runType = 'R' then
		select count(*) into idxExists
		from DE_SUBJECT_PROTEIN_DATA
		where trial_name = TrialId;
		
		if idxExists = 0 then
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'No data for TrialId in DE_SUBJECT_PROTEIN_DATA - procedure exiting',SQL%ROWCOUNT,stepCt,'Done');
			raise trial_missing;
		end if;
	end if;
*/
   
--	truncate tmp tables

	execute immediate('truncate table tm_wz.WT_SUBJECT_PROTEOMICS_LOGS');
	execute immediate('truncate table tm_wz.WT_SUBJECT_PROTEOMICS_CALCS');
	execute immediate('truncate table tm_wz.WT_SUBJECT_PROTEOMICS_MED');
         execute immediate('truncate table tm_cz.UPDATEZSCORE_PROTEOMICS');

	select count(*) 
	into idxExists
	from all_indexes
	where table_name = 'WT_SUBJECT_PROTEOMICS_LOGS'
	  and index_name = 'WT_SUBJECT_PROTEOMICS_LOGS_I1'
	  and owner = 'TM_WZ';
		
	if idxExists = 1 then
		execute immediate('drop index tm_wz.WT_SUBJECT_PROTEOMICS_LOGS_I1');		
	end if;
	
	select count(*) 
	into idxExists
	from all_indexes
	where table_name = 'WT_SUBJECT_PROTEOMICS_CALCS'
	  and index_name = 'WT_SUBJECT_PROTEOMICS_CALCS_I1'
	  and owner = 'TM_WZ';
		
	if idxExists = 1 then
		execute immediate('drop index tm_wz.WT_SUBJECT_PROTEOMICS_CALCS_I1');
	end if;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Truncate work tables in TM_WZ',0,stepCt,'Done');
	
	--	if dataType = L, use intensity_value as log_intensity
	--	if dataType = R, always use intensity_value


	if dataType = 'L' then
		insert into WT_SUBJECT_PROTEOMICS_LOGS 
			(probeset_id
			,intensity_value
			,assay_id
			,log_intensity
			,patient_id
		--	,sample_cd
			,subject_id
			)
			select probeset
				  ,round(intensity_value,6)  
				  ,assay_id 
				  ,round(intensity_value,4)
				  ,patient_id
			--	  ,sample_cd
				  ,subject_id
			from WT_SUBJECT_PROTEOMICS_PROBESET
			where trial_name = TrialId
                        union                             
                        select component
                        ,intensity
                        ,assay_id
                        ,intensity
                        ,patient_id
                        ,subject_id
                        from 
                        deapp.DE_SUBJECT_PROTEIN_DATA
                        where trial_name = TrialId;
           
		--end if;
	else	
                	insert into WT_SUBJECT_PROTEOMICS_LOGS 
			(probeset_id
			,intensity_value
			,assay_id
			,log_intensity
			,patient_id
		--	,sample_cd
			,subject_id
			)
			select probeset
				  ,round(intensity_value,6) 
				  ,assay_id 
				  ,round(log(2,intensity_value),4)
				  ,patient_id
		--		  ,sample_cd
				  ,subject_id
			from WT_SUBJECT_PROTEOMICS_PROBESET
			where trial_name = TrialId
                        union
                        select component
                        ,intensity
                        ,assay_id
                        ,log_intensity
                        ,patient_id
                        ,subject_id
                        from 
                        deapp.DE_SUBJECT_PROTEIN_DATA
                        where trial_name = TrialId;
--		end if;

	end if;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Loaded data for trial in TM_WZ wt_subject_mirna_logs',SQL%ROWCOUNT,stepCt,'Done');

	commit;
    
	execute immediate('create index tm_wz.WT_SUBJECT_PROTEOMICS_LOGS_I1 on tm_wz.WT_SUBJECT_PROTEOMICS_LOGS (trial_name, probeset_id) nologging  tablespace "INDX"');
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ WT_SUBJECT_PROTEOMICS_LOGS_I1',0,stepCt,'Done');
		
--	calculate mean_intensity, median_intensity, and stddev_intensity per experiment, probe

	insert into WT_SUBJECT_PROTEOMICS_CALCS
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
	from WT_SUBJECT_PROTEOMICS_LOGS d 
	group by d.trial_name 
			,d.probeset_id;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Calculate intensities for trial in TM_WZ WT_SUBJECT_PROTEOMICS_CALCS',SQL%ROWCOUNT,stepCt,'Done');

	commit;

	--execute immediate('create index tm_wz.wt_subject_proteomics_calcs_i1 on tm_wz.WT_SUBJECT_PROTEOMICS_CALCS (trial_name, probeset_id) nologging tablespace "INDX"');
	--stepCt := stepCt + 1;
	--cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ WT_SUBJECT_PROTEOMICS_CALCS',0,stepCt,'Done');
		
-- calculate zscore

	insert into WT_SUBJECT_PROTEOMICS_MED parallel 
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
	,subject_id
	)
	select d.probeset_id
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
    from WT_SUBJECT_PROTEOMICS_LOGS d 
		,WT_SUBJECT_PROTEOMICS_CALCS c 
    where d.probeset_id = c.probeset_id;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Calculate Z-Score for trial in TM_WZ WT_SUBJECT_PROTEOMICS_MED',SQL%ROWCOUNT,stepCt,'Done');

    commit;


        select count(*) into pExists from DE_SUBJECT_PROTEIN_DATA where trial_name=TrialId;

/*
	select count(*) into n
	select count(*) into nbrRecs
	from WT_SUBJECT_PROTEOMICS_MED;
	
	if nbrRecs > 10000000 then
		i2b2_mrna_index_maint('DROP',,jobId);
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Drop indexes on DEAPP DE_SUBJECT_PROTEIN_DATA',0,stepCt,'Done');
	else
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Less than 10M records, index drop bypassed',0,stepCt,'Done');
	end if;
*/	

delete deapp.DE_SUBJECT_PROTEIN_DATA where trial_name = TrialId;
commit;

	insert into DE_SUBJECT_PROTEIN_DATA
	(
	 trial_name
	,protein_annotation_id
	,component
	,gene_symbol
	,gene_id
	,assay_id
	,subject_id
	,intensity 
	,zscore
         ,log_intensity
	,patient_id
	)
	select TrialId
                 ,d.id
                 ,m.probeset_id 
                 ,d.uniprot_id
                 ,d.biomarker_id
                 ,m.assay_id
                 ,m.subject_id
	    --  ,decode(dataType,'R',m.intensity_value,'L',power(logBase, m.log_intensity),null)
		  ,round(m.intensity_value,6) as intensity
                  ,(CASE WHEN m.zscore < -2.5 THEN -2.5 WHEN m.zscore >  2.5 THEN  2.5 ELSE round(m.zscore,5) END)
                  ,round(m.log_intensity,4) as log_intensity
                   ,m.patient_id
	from WT_SUBJECT_PROTEOMICS_MED m
       , DEAPP.DE_PROTEIN_ANNOTATION d
        where d.peptide=m.probeset_id;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Insert data for trial in DEAPP DE_SUBJECT_PROTEIN_DATA',SQL%ROWCOUNT,stepCt,'Done');

  	commit;

    --update z-score values 


	/* if pExists > 0
    then
    I2B2_PROTEIN_INC_SUB_ZSCORE(TrialId);
	
		stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'update zscore in de_subject_protein_data ',0,stepCt,'Done');
        
     */
     
/*
     -- INSERT INTO UpdateZscore
     
     INSERT INTO UPDATEZSCORE_PROTEOMICS 
     (
      mean_value
      ,median_value
      ,stddev_value
      ,component
     )
      SELECT AVG(log_intensity) as mean_value
      ,median(log_intensity) as median_value
      ,STDDEV(log_intensity) as stddev_value
      ,component 
      FROM DE_SUBJECT_PROTEIN_DATA 
      WHERE trial_name = TrialId
      GROUP BY trial_name, component;  
      
      commit;


        update DE_SUBJECT_PROTEIN_DATA d 
        set d.zscore=(
        select ((d.log_intensity - UpdateZscore.median_value ) / UpdateZscore.stddev_value)
        from UPDATEZSCORE_PROTEOMICS UpdateZscore
        where d.component=UpdateZscore.component
        and UpdateZscore.stddev_value != 0
        and d.trial_name=TrialId);
       
        commit;
       
        update DE_SUBJECT_PROTEIN_DATA d 
        set d.zscore=0 where exists (select 1 from UPDATEZSCORE_PROTEOMICS where stddev_value=0)
        AND trial_name=TrialId ; 
        
       commit;
         
         update DE_SUBJECT_PROTEIN_DATA set zscore=round(CASE WHEN zscore < -2.5 THEN -2.5 WHEN zscore >  2.5 THEN  2.5 ELSE round(zscore,5) END,5)
                                        where trial_name=TrialID;
	
         commit;*/
        
       -- end if;
        
        
        



--	add indexes, if indexes were not dropped, procedure will not try and recreate
/*
	i2b2_mrna_index_maint('ADD',,jobId);
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Add indexes on DEAPP DE_SUBJECT_PROTEIN_DATA',0,stepCt,'Done');
*/
	
--	cleanup tmp_ files

    execute immediate('truncate table tm_wz.WT_SUBJECT_PROTEOMICS_LOGS');
    execute immediate('truncate table tm_wz.WT_SUBJECT_PROTEOMICS_CALCS');
    execute immediate('truncate table tm_wz.WT_SUBJECT_PROTEOMICS_MED');
     execute immediate('truncate table tm_cz.UPDATEZSCORE_PROTEOMICS');

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
/
