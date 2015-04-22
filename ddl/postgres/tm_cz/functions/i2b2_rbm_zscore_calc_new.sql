--
-- Name: i2b2_rbm_zscore_calc_new(character varying, character varying, bigint, character varying, bigint, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_rbm_zscore_calc_new(trial_id character varying, run_type character varying DEFAULT 'L'::character varying, currentjobid bigint DEFAULT 0, data_type character varying DEFAULT 'R'::character varying, log_base bigint DEFAULT 2, source_cd character varying DEFAULT NULL::character varying) RETURNS void
    LANGUAGE plpgsql
    AS $_$
DECLARE

/*************************************************************************
This Stored Procedure is used in ETL load RBM data
Date:12/04/2013
******************************************************************/

  TrialID varchar(50);
  sourceCD	varchar(50);
  sqlText varchar(2000);
  runType varchar(10);
  dataType varchar(10);
  stgTrial varchar(50);
  idxExists bigint;
  pExists	bigint;
  nbrRecs bigint;
  logBase bigint;
   
  --Audit variables
  newJobFlag integer(1);
  databaseName varchar(100);
  procedureName varchar(100);
  jobID bigint;
  stepCt bigint;
  
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
  
--	For Load, make sure that the TrialId passed as parameter is the same as the trial in stg_subject_mrna_data
--	If not, raise exception

	if runType = 'L' then
		select distinct trial_name into stgTrial
		from WT_SUBJECT_RBM_PROBESET;
		
		if stgTrial != TrialId then
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'TrialId not the same as trial in WT_SUBJECT_RBM_PROBESET - procedure exiting'
,SQL%ROWCOUNT,stepCt,'Done');
			raise trial_mismatch;
		end if;
	end if;

	--remove Reload processing
--	For Reload, make sure that the TrialId passed as parameter has data in de_subject_rbm_data
--	If not, raise exception

	if runType = 'R' then 
		select count(*) into idxExists
		from de_subject_rbm_data
		where trial_name = TrialId;
		
		if idxExists = 0 then
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'No data for TrialId in de_subject_rbm_data - procedure exiting'
,SQL%ROWCOUNT,stepCt,'Done');
			raise trial_missing;
		end if;
	end if;

   
--	truncate tmp tables

	EXECUTE('truncate table tm_wz.wt_subject_rbm_logs');
	EXECUTE('truncate table tm_wz.wt_subject_rbm_calcs');
	EXECUTE('truncate table tm_wz.wt_subject_rbm_med');

	select count(*) 
	into idxExists
	from all_indexes
	where table_name = 'WT_SUBJECT_RBM_LOGS'
	  and index_name = 'WT_SUBJECT_RBM_LOGS_I1'
	  and owner = 'TM_WZ';
		
	if idxExists = 1 then
		EXECUTE('drop index tm_wz.wt_subject_rbm_logs_i1');		
	end if;
	
	select count(*) 
	into idxExists
	from all_indexes
	where table_name = 'WT_SUBJECT_RBM_CALCS'
	  and index_name = 'WT_SUBJECT_RBM_CALCS_I1'
	  and owner = 'TM_WZ';
		
	if idxExists = 1 then
		EXECUTE('drop index tm_wz.wt_subject_rbm_calcs_i1');
	end if;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Truncate work tables in TM_WZ',0,stepCt,'Done');
	
	--	if dataType = L, use intensity_value as log_intensity
	--	if dataType = R, always use intensity_value


	if dataType = 'L' then
		insert into wt_subject_rbm_logs 
			(probeset_id
			,intensity_value
			,assay_id
			,log_intensity
			,patient_id
		--	,sample_cd
		--	,subject_id
			)
			PERFORM probeset
				  ,intensity_value  
				  ,assay_id 
				  ,intensity_value
				  ,patient_id
			--	  ,sample_cd
			--	  ,subject_id
			from wt_subject_rbm_probeset
			where trial_name = TrialId;
           
		--end if;
	else	
                	insert into wt_subject_rbm_logs 
			(probeset_id
			,intensity_value
			,assay_id
			,log_intensity
			,patient_id
		--	,sample_cd
		--	,subject_id
			)
			PERFORM probeset
				  ,intensity_value 
				  ,assay_id 
				  ,log(2,intensity_value)
				  ,patient_id
		--		  ,sample_cd
		--		  ,subject_id
			from wt_subject_rbm_probeset
			where trial_name = TrialId;
--		end if;

	end if;

	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Loaded data for trial in TM_WZ wt_subject_rbm_logs',SQL%ROWCOUNT,stepCt,'Done');

	commit;
    
	EXECUTE('create index tm_wz.wt_subject_rbm_logs_i1 on tm_wz.wt_subject_rbm_logs (trial_name, probeset_id) nologging  tablespace "INDX"');
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ wt_subject_rbm_logs',0,stepCt,'Done');
		
--	calculate mean_intensity, median_intensity, and stddev_intensity per experiment, probe

	insert into wt_subject_rbm_calcs
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
	from wt_subject_rbm_logs d 
	group by d.trial_name 
			,d.probeset_id;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Calculate intensities for trial in TM_WZ wt_subject_rbm_calcs',SQL%ROWCOUNT,stepCt,'Done');

	commit;

	EXECUTE('create index tm_wz.wt_subject_rbm_calcs_i1 on tm_wz.wt_subject_rbm_calcs (trial_name, probeset_id) nologging tablespace "INDX"');
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ wt_subject_rbm_calcs',0,stepCt,'Done');
		
-- calculate zscore

	insert into wt_subject_rbm_med parallel 
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
	PERFORM d.probeset_id
		  ,d.intensity_value 
		  ,d.log_intensity 
		  ,d.assay_id  
		  ,c.mean_intensity 
		  ,c.stddev_intensity 
		  ,c.median_intensity 
		  ,(CASE WHEN stddev_intensity=0 THEN 0 ELSE (log_intensity - median_intensity ) / stddev_intensity END)
		  ,d.patient_id
	--	  ,d.sample_cd
	--	  ,d.subject_id
    from wt_subject_rbm_logs d 
		,wt_subject_rbm_calcs c 
    where d.probeset_id = c.probeset_id;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Calculate Z-Score for trial in TM_WZ wt_subject_rbm_med',SQL%ROWCOUNT,stepCt,'Done');

    commit;

/*
	select count(*) into n
	select count(*) into nbrRecs
	from wt_subject_rbm_med;
	
	if nbrRecs > 10000000 then
		i2b2_mrna_index_maint('DROP',,jobId);
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Drop indexes on DEAPP de_subject_rbm_data',0,stepCt,'Done');
	else
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Less than 10M records, index drop bypassed',0,stepCt,'Done');
	end if;
*/
	


	insert into de_subject_rbm_data 
	(trial_name
        --,rbm_annotation_id
	,antigen_name
	,patient_id
        ,gene_symbol
        ,gene_id
        ,assay_id
        ,concept_cd
        ,value
        ,normalized_value
        ,unit    
	,zscore
	)
	PERFORM TrialId
               --,a.id ---rbm_annotation_id
              ,trim(substr(m.probeset_id,1,instr(m.probeset_id,'(')-1)) --m.probeset_id 
              ,m.patient_id
              ,a.gene_symbol  -- gene-symbol 
              ,a.gene_id  --gene_id
              ,m.assay_id
              ,d.concept_code --concept_cd
              ,m.intensity_value
              ,round(case when dataType = 'R' then m.intensity_value
				when dataType = 'L' 
				then case when logBase = -1 then null else power(logBase, m.log_intensity) end
				else null
				end,4) as normalized_value
	    --  ,decode(dataType,'R',m.intensity_value,'L',power(logBase, m.log_intensity),null)
              
              ,trim(substr(m.probeset_id ,instr(m.probeset_id ,'(',-1,1),length(m.probeset_id )))
              ,(CASE WHEN m.zscore < -2.5 THEN -2.5 WHEN m.zscore >  2.5 THEN  2.5 ELSE m.zscore END)
          --	  ,m.sample_id
	--	  ,m.subject_id
	from wt_subject_rbm_med m
        ,wt_subject_rbm_probeset p
        ,DE_RBM_ANNOTATION a
        ,de_subject_sample_mapping d
        where 
        trim(substr(p.probeset,1,instr(p.probeset,'(')-1)) =trim(a.antigen_name) 
        --and 
      and   d.subject_id=p.subject_id
        and p.platform=a.gpl_id
        and m.assay_id=p.assay_id
        and d.platform=p.platform
        and d.patient_id=p.patient_id
        and d.concept_code in (select concept_cd from  i2b2demodata.concept_dimension where concept_cd=d.concept_code)
        and d.trial_name=TrialId
        
        and p.patient_id=m.patient_id
        and p.probeset=m.probeset_id;
        
        insert into DEAPP.DE_RBM_DATA_ANNOTATION_JOIN
select d.id, ann.id from deapp.de_subject_rbm_data d
inner join deapp.de_rbm_annotation ann on ann.antigen_name = d.antigen_name
inner join deapp.de_subject_sample_mapping ssm on ssm.assay_id = d.assay_id and ann.gpl_id = ssm.gpl_id
where not exists( select * from deapp.de_rbm_data_annotation_join j where j.data_id = d.id AND j.annotation_id = ann.id );

        /*
        select distinct TrialId
               ,a.id ---rbm_annotation_id
              ,trim(substr(m.probeset_id,1,instr(m.probeset_id,'(')-1)) --m.probeset_id 
              ,m.patient_id
              ,a.gene_symbol  -- gene-symbol 
              ,a.gene_id  --gene_id
              ,m.assay_id
              ,d.concept_code --concept_cd
              ,m.intensity_value
              ,round(case when dataType = 'R' then m.intensity_value
				when dataType = 'L' 
				then case when logBase = -1 then null else power(logBase, m.log_intensity) end
				else null
				end,4) as normalized_value
	    --  ,decode(dataType,'R',m.intensity_value,'L',power(logBase, m.log_intensity),null)
              
              ,trim(substr(m.probeset_id ,instr(m.probeset_id ,'('),length(m.probeset_id )))
              ,(CASE WHEN m.zscore < -2.5 THEN -2.5 WHEN m.zscore >  2.5 THEN  2.5 ELSE m.zscore END)
          --	  ,m.sample_id
	--	  ,m.subject_id
         from wt_subject_rbm_med m, DE_RBM_ANNOTATION a,de_subject_sample_mapping d
         --(select distinct intensity_value from wt_subject_rbm_probeset p) x
        where a.antigen_name = trim(substr(m.probeset_id,1,instr(m.probeset_id,'(')-1)) and m.patient_id = d.patient_id  
        --and d.assay_id  = m.assay_id
        and d.trial_name = TrialId
        and d.source_cd = source_cd;
        --and d.gpl_id = a.gpl_id;
        --and x.intensity_value = m.intensity_value;
    */
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Insert data for trial in DEAPP de_subject_rbm_data',SQL%ROWCOUNT,stepCt,'Done');

  	commit;

--	add indexes, if indexes were not dropped, procedure will not try and recreate
/*
	i2b2_mrna_index_maint('ADD',,jobId);
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Add indexes on DEAPP de_subject_rbm_data',0,stepCt,'Done');
*/
	
--	cleanup tmp_ files

	--execute immediate('truncate table tm_wz.wt_subject_rbm_logs');
	--execute immediate('truncate table tm_wz.wt_subject_rbm_calcs');
	--execute immediate('truncate table tm_wz.wt_subject_rbm_med');

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

--
-- Name: i2b2_rbm_zscore_calc_new(character varying, character varying, character varying, character varying, bigint, character varying, bigint, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_rbm_zscore_calc_new(trial_id character varying, partition_name character varying, partition_indx character varying, run_type character varying DEFAULT 'L'::character varying, currentjobid bigint DEFAULT 0, data_type character varying DEFAULT 'R'::character varying, log_base bigint DEFAULT 2, source_cd character varying DEFAULT NULL::character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE

/*************************************************************************
This Stored Procedure is used in ETL load RBM data
Date:12/04/2013
******************************************************************/

  TrialID varchar(50);
  sourceCD	varchar(50);
  sqlText varchar(2000);
  runType varchar(10);
  dataType varchar(10);
  partitionName varchar(200);
  partitionindx varchar(200);
  stgTrial varchar(50);
  idxExists bigint;
  pExists	bigint;
  nbrRecs bigint;
  logBase bigint;
   
  --Audit variables
  newJobFlag integer;
  databaseName varchar(100);
  procedureName varchar(100);
  jobID bigint;
  stepCt bigint;
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
	
	partitionindx := partition_indx;
	partitionName := partition_name;
	  
  --Set Audit Parameters
  newJobFlag := 0; -- False (Default)
  jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2B2_LOAD_RBM_DATA';

  --Audit JOB Initialization
  --If Job ID does not exist, then this is a single procedure run and we need to create it
  IF(coalesce(jobID::text, '') = '' or jobID < 1)
  THEN
    newJobFlag := 1; -- True
    select cz_start_audit (procedureName, databaseName, jobID) into rtnCd;
  END IF;
   
  stepCt := 0;
  
  	select count(*) into pExists
	from information_schema.tables
	where table_name = partitionindx;

	if pExists = 0 then
	sqlText := 'create table ' || partitionName || ' ( constraint rbm_' || partitionId::text || '_check check ( partition_id = ' || partitionId::text ||
	')) inherits (deapp.de_subject_rbm_data)';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Create partition ' || partitionName,1,stepCt,'Done') into rtnCd;
	else
	sqlText := 'drop index if exists ' || partitionIndx || '_idx1';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	sqlText := 'drop index if exists ' || partitionIndx || '_idx2';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	sqlText := 'drop index if exists ' || partitionIndx || '_idx3';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	sqlText := 'drop index if exists ' || partitionIndx || '_idx4';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Drop indexes on ' || partitionName,1,stepCt,'Done') into rtnCd;
	sqlText := 'truncate table ' || partitionName;
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Truncate ' || partitionName,1,stepCt,'Done') into rtnCd;
	end if;
  
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Starting zscore calc for ' || TrialId || ' RunType: ' || runType || ' dataType: ' || dataType,0,stepCt,'Done') into rtnCd;
  
	if runType != 'L' then
		stepCt := stepCt + 1;
		select cz_write_audit(jobId,databaseName,procedureName,'Invalid runType passed - procedure exiting'
,0,stepCt,'Done') into rtnCd;
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		select cz_end_audit (jobId,'FAIL') into rtnCd;
		return;
	end if;
  
--	For Load, make sure that the TrialId passed as parameter is the same as the trial in stg_subject_mrna_data
--	If not, raise exception

	if runType = 'L' then
		select distinct trial_name into stgTrial
		from tm_wz.WT_SUBJECT_RBM_PROBESET;
		
		if stgTrial != TrialId then
			stepCt := stepCt + 1;
			select cz_write_audit(jobId,databaseName,procedureName,'TrialId not the same as trial in WT_SUBJECT_RBM_PROBESET - procedure exiting'
,0,stepCt,'Done') into rtnCd;
			select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
			select cz_end_audit (jobId,'FAIL') into rtnCd;
			return;
		end if;
	end if;

	--remove Reload processing
--	For Reload, make sure that the TrialId passed as parameter has data in de_subject_rbm_data
--	If not, raise exception

	if runType = 'R' then 
		select count(*) into idxExists
		from deapp.de_subject_rbm_data
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

   
--	truncate tmp tables

	EXECUTE('truncate table tm_wz.wt_subject_rbm_logs');
	EXECUTE('truncate table tm_wz.wt_subject_rbm_calcs');
	EXECUTE('truncate table tm_wz.wt_subject_rbm_med');

	EXECUTE('drop index if exists tm_wz.wt_subject_rbm_logs_i1');		
		
	if idxExists = 1 then
		EXECUTE('drop index if exists tm_wz.wt_subject_rbm_calcs_i1');
	end if;

	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Truncate work tables in TM_WZ',0,stepCt,'Done') into rtnCd;
	
	--	if dataType = L, use intensity_value as log_intensity
	--	if dataType = R, always use intensity_value

	begin
		if dataType = 'L' then
			insert into tm_wz.wt_subject_rbm_logs 
				(probeset_id
				,intensity_value
				,assay_id
				,log_intensity
				,patient_id
				)
				select probeset
					  ,intensity_value  
					  ,assay_id 
					  ,intensity_value
					  ,patient_id
				from tm_wz.wt_subject_rbm_probeset
				where trial_name = TrialId;
			   
			--end if;
		else	
			insert into tm_wz.wt_subject_rbm_logs 
				(probeset_id
				,intensity_value
				,assay_id
				,log_intensity
				,patient_id
				)
				select probeset
					  ,intensity_value 
					  ,assay_id 
					  ,log(2,intensity_value)
					  ,patient_id
				from wt_subject_rbm_probeset
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
	select cz_write_audit(jobId,databaseName,procedureName,'Loaded data for trial in TM_WZ wt_subject_rbm_logs',rowCt,stepCt,'Done') into rtnCd;
    
	EXECUTE('create index tm_wz.wt_subject_rbm_logs_i1 on tm_wz.wt_subject_rbm_logs (trial_name, probeset_id) nologging  tablespace "INDX"');
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ wt_subject_rbm_logs',0,stepCt,'Done') into rtnCd;
		
--	calculate mean_intensity, median_intensity, and stddev_intensity per experiment, probe
	begin
		insert into tm_wz.wt_subject_rbm_calcs
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
		from tm_wz.wt_subject_rbm_logs d 
		group by d.trial_name 
				,d.probeset_id;
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
	select cz_write_audit(jobId,databaseName,procedureName,'Calculate intensities for trial in TM_WZ wt_subject_rbm_calcs',SQL%ROWCOUNT,stepCt,'Done') into rtnCd;


	EXECUTE('create index tm_wz.wt_subject_rbm_calcs_i1 on tm_wz.wt_subject_rbm_calcs (trial_name, probeset_id) nologging tablespace "INDX"');
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ wt_subject_rbm_calcs',0,stepCt,'Done') into rtnCd;
		
-- calculate zscore
	begin
		insert into tm_wz.wt_subject_rbm_med 
		(probeset_id
		,intensity_value
		,log_intensity
		,assay_id
		,mean_intensity
		,stddev_intensity
		,median_intensity
		,zscore
		,patient_id
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
		from tm_wz.wt_subject_rbm_logs d 
			,tm_wz.wt_subject_rbm_calcs c 
		where d.probeset_id = c.probeset_id;
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
	select cz_write_audit(jobId,databaseName,procedureName,'Calculate Z-Score for trial in TM_WZ wt_subject_rbm_med',rowCt,stepCt,'Done') into rtnCd;

	
	-- insert into de_subject_rbm_data when dataType is T (transformed)

	execute ('drop index if exists tm_wz.wt_subject_mrna_logs_i1');
	execute ('drop index if exists tm_wz.wt_subject_mrna_calcs_i1');
	
	sqlText := 'insert into ' || partitionName || 
	'(trial_name, antigen_name, patient_id, gene_symbol, gene_id, assay_id ' ||
        ',concept_cd, value, normalized_value, unit, zscore) ' ||
	'select TrialId ' ||
              ',trim(substr(m.probeset_id,1,instr(m.probeset_id,''('')-1)) --m.probeset_id  ' ||
              ',m.patient_id ' ||
              ',a.gene_symbol  -- gene-symbol  ' ||
              ',a.gene_id  --gene_id ' ||
              ',m.assay_id ' ||
              ',d.concept_code --concept_cd ' ||
              ',m.intensity_value ' ||
              ',round(case when dataType = ''R'' then m.intensity_value ' ||
				'when dataType = ''L''  ' ||
				'then case when logBase = -1 then null else power(logBase, m.log_intensity) end ' ||
				'else null ' ||
				'end,4) as normalized_value ' ||
              ',trim(substr(m.probeset_id ,instr(m.probeset_id ,''('',-1,1),length(m.probeset_id ))) ' ||
              ',(CASE WHEN m.zscore < -2.5 THEN -2.5 WHEN m.zscore >  2.5 THEN  2.5 ELSE m.zscore END) ' ||
	'from wt_subject_rbm_med m ' ||
        ',wt_subject_rbm_probeset p ' ||
        ',DE_RBM_ANNOTATION a ' ||
        ',de_subject_sample_mapping d ' ||
        'where  ' ||
        'trim(substr(p.probeset,1,instr(p.probeset,''('')-1)) =trim(a.antigen_name)  ' ||
      'and   d.subject_id=p.subject_id ' ||
        'and p.platform=a.gpl_id ' ||
        'and m.assay_id=p.assay_id ' ||
        'and d.platform=p.platform ' ||
        'and d.patient_id=p.patient_id ' ||
        'and d.concept_code in (select concept_cd from  i2b2demodata.concept_dimension where concept_cd=d.concept_code) ' ||
        'and d.trial_name=TrialId  ' ||
        'and p.patient_id=m.patient_id ' ||
        'and p.probeset=m.probeset_id ' ;
		
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	get diagnostics rowCt := ROW_COUNT;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Inserted data into ' || partitionName,rowCt,stepCt,'Done') into rtnCd;

	insert into DEAPP.DE_RBM_DATA_ANNOTATION_JOIN
	select d.id, ann.id from deapp.de_subject_rbm_data d
	inner join deapp.de_rbm_annotation ann on ann.antigen_name = d.antigen_name
	inner join deapp.de_subject_sample_mapping ssm on ssm.assay_id = d.assay_id and ann.gpl_id = ssm.gpl_id
	where not exists( select * from deapp.de_rbm_data_annotation_join j where j.data_id = d.id AND j.annotation_id = ann.id );

        
	
	-- create indexes on partition

	sqlText := ' create index ' || partitionIndx || '_idx1 on ' || partitionName || ' using btree (partition_id) tablespace indx';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	sqlText := ' create index ' || partitionIndx || '_idx2 on ' || partitionName || ' using btree (assay_id) tablespace indx';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	sqlText := ' create index ' || partitionIndx || '_idx3 on ' || partitionName || ' using btree (probeset_id) tablespace indx';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	sqlText := ' create index ' || partitionIndx || '_idx4 on ' || partitionName || ' using btree (assay_id, probeset_id) tablespace indx';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
    
    ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1
  THEN 
    select cz_end_audit (jobID, 'SUCCESS') into rtnCd; 
  END IF;

END;
 
$$;

--
-- Name: i2b2_rbm_zscore_calc_new(character varying, character varying, character varying, numeric, character varying, bigint, character varying, bigint, character varying); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_rbm_zscore_calc_new(trial_id character varying, partition_name character varying, partition_indx character varying, partitionid numeric, run_type character varying DEFAULT 'L'::character varying, currentjobid bigint DEFAULT 0, data_type character varying DEFAULT 'R'::character varying, log_base bigint DEFAULT 2, source_cd character varying DEFAULT NULL::character varying) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE

/*************************************************************************
This Stored Procedure is used in ETL load RBM data
Date:12/04/2013
******************************************************************/

  TrialID varchar(50);
  sourceCD	varchar(50);
  sqlText varchar(2000);
  runType varchar(10);
  dataType varchar(10);
  partitionName varchar(200);
  partitionindx varchar(200);
  stgTrial varchar(50);
  idxExists bigint;
  pExists	bigint;
  nbrRecs bigint;
  logBase bigint;
   
  --Audit variables
  newJobFlag integer;
  databaseName varchar(100);
  procedureName varchar(100);
  jobID bigint;
  stepCt bigint;
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
	
	partitionindx := partition_indx;
	partitionName := partition_name;
	  
  --Set Audit Parameters
  newJobFlag := 0; -- False (Default)
  jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'i2b2_rbm_zscore_calc_new';

  --Audit JOB Initialization
  --If Job ID does not exist, then this is a single procedure run and we need to create it
  IF(coalesce(jobID::text, '') = '' or jobID < 1)
  THEN
    newJobFlag := 1; -- True
    select cz_start_audit (procedureName, databaseName, jobID) into rtnCd;
  END IF;
   
  stepCt := 0;
  
  	select count(*) into pExists
	from information_schema.tables
	where table_name = partitionindx;

	if pExists = 0 then
	sqlText := 'create table ' || partitionName || ' ( constraint rbm_' || partitionId::text || '_check check ( partition_id = ' || partitionId::text ||
	')) inherits (deapp.de_subject_rbm_data)';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Create partition ' || partitionName,1,stepCt,'Done') into rtnCd;
	else
	select tm_cz.remove_table_keys('deapp', replace(partitionName, 'deapp.', ''));
	select tm_cz.remove_table_indexes('deapp', replace(partitionName, 'deapp.', ''));
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Drop indexes on ' || partitionName,1,stepCt,'Done') into rtnCd;
	sqlText := 'truncate table ' || partitionName;
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Truncate ' || partitionName,1,stepCt,'Done') into rtnCd;
	end if;
  
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Starting zscore calc for ' || TrialId || ' RunType: ' || runType || ' dataType: ' || dataType,0,stepCt,'Done') into rtnCd;
  
	if runType != 'L' then
		stepCt := stepCt + 1;
		select cz_write_audit(jobId,databaseName,procedureName,'Invalid runType passed - procedure exiting'
,0,stepCt,'Done') into rtnCd;
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		select cz_end_audit (jobId,'FAIL') into rtnCd;
		return;
	end if;
  
--	For Load, make sure that the TrialId passed as parameter is the same as the trial in stg_subject_mrna_data
--	If not, raise exception

	if runType = 'L' then
		select distinct trial_name into stgTrial
		from tm_wz.WT_SUBJECT_RBM_PROBESET;
		
		if stgTrial != TrialId then
			stepCt := stepCt + 1;
			select cz_write_audit(jobId,databaseName,procedureName,'TrialId not the same as trial in WT_SUBJECT_RBM_PROBESET - procedure exiting'
,0,stepCt,'Done') into rtnCd;
			select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
			select cz_end_audit (jobId,'FAIL') into rtnCd;
			return;
		end if;
	end if;

	--remove Reload processing
--	For Reload, make sure that the TrialId passed as parameter has data in de_subject_rbm_data
--	If not, raise exception

	if runType = 'R' then 
		select count(*) into idxExists
		from deapp.de_subject_rbm_data
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

   
--	truncate tmp tables

	EXECUTE('truncate table tm_wz.wt_subject_rbm_logs');
	EXECUTE('truncate table tm_wz.wt_subject_rbm_calcs');
	EXECUTE('truncate table tm_wz.wt_subject_rbm_med');

	EXECUTE('drop index if exists tm_wz.wt_subject_rbm_logs_i1');		
		
	EXECUTE('drop index if exists tm_wz.wt_subject_rbm_calcs_i1');
	
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Truncate work tables in TM_WZ',0,stepCt,'Done') into rtnCd;
	
	--	if dataType = L, use intensity_value as log_intensity
	--	if dataType = R, always use intensity_value

	begin
		if dataType = 'L' then
			insert into tm_wz.wt_subject_rbm_logs 
				(probeset_id
				,intensity_value
				,assay_id
				,log_intensity
				,patient_id
				)
				select probeset
					  ,intensity_value  
					  ,assay_id 
					  ,intensity_value
					  ,patient_id
				from tm_wz.wt_subject_rbm_probeset
				where trial_name = TrialId;
			   
			--end if;
		else	
			insert into tm_wz.wt_subject_rbm_logs 
				(probeset_id
				,intensity_value
				,assay_id
				,log_intensity
				,patient_id
				)
				select probeset
					  ,intensity_value 
					  ,assay_id 
					  ,CASE WHEN intensity_value <= 0 THEN log(2,(intensity_value + 0.001)) ELSE log(2,intensity_value) END
					  ,patient_id
				from tm_wz.wt_subject_rbm_probeset
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
	select cz_write_audit(jobId,databaseName,procedureName,'Loaded data for trial in TM_WZ wt_subject_rbm_logs',rowCt,stepCt,'Done') into rtnCd;
    
	EXECUTE('create index wt_subject_rbm_logs_i1 on tm_wz.wt_subject_rbm_logs (trial_name, probeset_id) tablespace "indx"');
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ wt_subject_rbm_logs',0,stepCt,'Done') into rtnCd;
		
--	calculate mean_intensity, median_intensity, and stddev_intensity per experiment, probe
	begin
		insert into tm_wz.wt_subject_rbm_calcs
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
		from tm_wz.wt_subject_rbm_logs d 
		group by d.trial_name 
				,d.probeset_id;
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
	select cz_write_audit(jobId,databaseName,procedureName,'Calculate intensities for trial in TM_WZ wt_subject_rbm_calcs',rowCt,stepCt,'Done') into rtnCd;


	EXECUTE('create index wt_subject_rbm_calcs_i1 on tm_wz.wt_subject_rbm_calcs (trial_name, probeset_id) tablespace "indx"');
	stepCt := stepCt + 1;
	select cz_write_audit(jobId,databaseName,procedureName,'Create index on TM_WZ wt_subject_rbm_calcs',0,stepCt,'Done') into rtnCd;
		
-- calculate zscore
	begin
		insert into tm_wz.wt_subject_rbm_med 
		(probeset_id
		,intensity_value
		,log_intensity
		,assay_id
		,mean_intensity
		,stddev_intensity
		,median_intensity
		,zscore
		,patient_id
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
		from tm_wz.wt_subject_rbm_logs d 
			,tm_wz.wt_subject_rbm_calcs c 
		where d.probeset_id = c.probeset_id;
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
	select cz_write_audit(jobId,databaseName,procedureName,'Calculate Z-Score for trial in TM_WZ wt_subject_rbm_med',rowCt,stepCt,'Done') into rtnCd;

	
	-- insert into de_subject_rbm_data when dataType is T (transformed)

	
	sqlText := 'insert into ' || partitionName || 
	'(partition_id, trial_name, antigen_name, patient_id, gene_symbol, gene_id, assay_id ' ||
        ',concept_cd, value, normalized_value, unit, zscore, id) ' ||
	'select ' || partitioniD::text || ', ''' || TrialId || '''' ||
              ',trim(substr(m.probeset_id,1,instr(m.probeset_id,''('')-1)) ' ||
              ',m.patient_id ' ||
              ',a.gene_symbol  ' ||
              ',a.gene_id::integer  ' ||
              ',m.assay_id ' ||
              ',d.concept_code ' ||
              ',m.intensity_value ' ||
              ',round(case when ''' || dataType || ''' = ''R'' then m.intensity_value::numeric ' ||
				'when ''' || dataType || ''' = ''L''  ' ||
				'then case when ''' || logBase || ''' = -1 then null else power( ''' || logBase || ''' , m.log_intensity)::numeric end ' ||
				'else null ' ||
				'end,4) as normalized_value ' ||
              ',trim(substr(m.probeset_id ,instr(m.probeset_id ,''('',-1,1),length(m.probeset_id ))) ' ||
              ',(CASE WHEN m.zscore < -2.5 THEN -2.5 WHEN m.zscore >  2.5 THEN  2.5 ELSE m.zscore END) ' ||
			  ',nextval(''deapp.RBM_ANNOTATION_ID'') '||
	'from tm_wz.wt_subject_rbm_med m ' ||
        ',tm_wz.wt_subject_rbm_probeset p ' ||
        ',deapp.DE_RBM_ANNOTATION a ' ||
        ',deapp.de_subject_sample_mapping d ' ||
        'where  ' ||
        'trim(substr(p.probeset,1,instr(p.probeset,''('')-1)) =trim(a.antigen_name)  ' ||
      'and   d.subject_id=p.subject_id ' ||
        'and p.platform=a.gpl_id ' ||
        'and m.assay_id=p.assay_id ' ||
        'and d.gpl_id=p.platform ' ||
        'and d.patient_id=p.patient_id ' ||
        'and d.concept_code in (select concept_cd from  i2b2demodata.concept_dimension where concept_cd=d.concept_code) ' ||
        'and d.trial_name=''' || TrialId || '''' ||
        'and p.patient_id=m.patient_id ' ||
        'and p.probeset=m.probeset_id ' ;
		
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	get diagnostics rowCt := ROW_COUNT;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Inserted data into ' || partitionName,rowCt,stepCt,'Done') into rtnCd;
	
	sqlText := ' alter table ' || partitionName || ' add constraint ' || partitionIndx || '_pk primary key (id);';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;

	sqlText := ' alter table ' || partitionName || ' add constraint ' || partitionIndx || '_assay_id_fk foreign key (assay_id) references deapp.de_subject_sample_mapping(assay_id) on delete cascade;';
	raise notice 'sqlText= %', sqlText;
	execute sqlText;
	stepCt := stepCt + 1;
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Created keys for ' || partitionName,rowCt,stepCt,'Done') into rtnCd;

	insert into DEAPP.DE_RBM_DATA_ANNOTATION_JOIN
	select d.id, ann.id from deapp.de_subject_rbm_data d
	inner join deapp.de_rbm_annotation ann on ann.antigen_name = d.antigen_name
	inner join deapp.de_subject_sample_mapping ssm on ssm.assay_id = d.assay_id and ann.gpl_id = ssm.gpl_id
	where not exists( select * from deapp.de_rbm_data_annotation_join j where j.data_id = d.id AND j.annotation_id = ann.id );
    
    ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1
  THEN 
    select cz_end_audit (jobID, 'SUCCESS') into rtnCd; 
  END IF;

END;
 
$$;

