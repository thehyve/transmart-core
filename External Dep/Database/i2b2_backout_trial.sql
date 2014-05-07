create or replace 
PROCEDURE tm_cz.I2B2_BACKOUT_TRIAL 
(
  trial_id VARCHAR2
 ,path_string varchar2
 ,currentJobID NUMBER := null
)
AS

--	JEA@20100106	New
--	JEA@20100112	Added removal of SECURITY records from observation_fact
--	JEA@20120313	Added colon to study on patient_dimension delete
--	JEA@20120416	Only delete omic data if omic_source_study is null or same as TrialId

  TrialID	varchar2(100);
  TrialType VARCHAR2(250);
  pExists	number;
  sqlText	varchar2(1000);
  
  
  --Audit variables
  newJobFlag INTEGER(1);
  databaseName VARCHAR(100);
  procedureName VARCHAR(100);
  jobID number(18,0);
  stepCt number(18,0);

BEGIN
  --TrialID := upper(trial_id);
  TrialId := trial_id;
  
  stepCt := 0;
	
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
  
  if path_string != ''  or path_string != '%'
  then 
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Starting i2b2_backout_trial',0,stepCt,'Done');

	--	delete all i2b2 nodes
	
	i2b2_delete_all_nodes(path_string,jobId);
	
	--	delete any i2b2_tag data
	
	delete from i2b2_tags
	where path like path_string || '%';
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from I2B2METADATA i2b2_tags',SQL%ROWCOUNT,stepCt,'Done');
	commit;
	
	--	delete clinical data
	
	delete from lz_src_clinical_data
	where study_id = trialId;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from lz_src_clinical_data',SQL%ROWCOUNT,stepCt,'Done');
	commit;
		
	--	delete observation_fact SECURITY data, do before patient_dimension delete
	
	delete from observation_fact f
	where f.concept_cd = 'SECURITY'
	  and f.patient_num in
	     (select distinct p.patient_num from patient_dimension p
		  where p.sourcesystem_cd like trialId || ':%');
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete SECURITY data for trial from I2B2DEMODATA observation_fact',SQL%ROWCOUNT,stepCt,'Done');
	commit;	
	
	--	delete patient data
	
	delete from patient_dimension
	where sourcesystem_cd like trialId || ':%';
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from I2B2DEMODATA patient_dimension',SQL%ROWCOUNT,stepCt,'Done');
	commit;
	
	delete from patient_trial
	where trial=  trialId;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from I2B2DEMODATA patient_trial',SQL%ROWCOUNT,stepCt,'Done');
	commit;
	
	--	delete gene expression data
	
	select count(*) into pExists
	from de_subject_sample_mapping
	where trial_name = TrialId
	  and platform = 'MRNA_AFFYMETRIX'
	  and trial_name = TrialId
	  --and coalesce(omic_source_study,trial_name) = TrialId
	  ;
	  
	if pExists > 1 then
		select count(*) into pExists
		from all_tables
		where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
		  and partitioned = 'YES';
		  
		if pExists = 0 then
			--	table not partitioned, do delete
			delete from de_subject_microarray_data
			where trial_name = TrialId;
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from de_subject_sample_mapping',SQL%ROWCOUNT,stepCt,'Done');
			commit;			
		else
			select count(*) into pExists
			from all_tab_partitions
			where table_name = 'DE_SUBJECT_MICROARRAY_DATA'
			  and partition_name = TrialId;
			  
			if pExists > 0 then			
				sqlText := 'alter table deapp.de_subject_microarray_data drop PARTITION "' || TrialID || '"';
				execute immediate(sqlText);
				stepCt := stepCt + 1;
				cz_write_audit(jobId,databaseName,procedureName,'Dropped partition from de_subject_microarray_data',0,stepCt,'Done');
			end if;
		end if;
	else
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'No gene expression data to be deleted',0,stepCt,'Done');
	end if;
		
	--	check for SNP data	
	
	select count(*) into pExists
	from de_subject_sample_mapping
	where trial_name = TrialId
	  and platform = 'SNP'
	  and trial_name = TrialId;
	  
	if pexists > 0 then
		--	delete SNP data
		
		delete from de_snp_data_dataset_loc
		where trial_name = TrialId;
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from de_snp_data_dataset_loc',SQL%ROWCOUNT,stepCt,'Done');
		commit;
		
		delete from de_snp_data_by_patient
		where trial_name = TrialId;
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from de_snp_data_by_patient',SQL%ROWCOUNT,stepCt,'Done');
		commit;
		
		delete from deapp.de_snp_calls_by_gsm s
		where s.patient_num in 
			 (select distinct x.patient_id
			  from de_subject_sample_mapping x
			  where x.trial_name = TrialId
			    and x.platform = 'SNP');
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from de_snp_calls_by_gsm',SQL%ROWCOUNT,stepCt,'Done');
		commit;
		
		delete from deapp.de_snp_copy_number s
		where s.patient_num in 
			 (select distinct x.patient_id
			  from de_subject_sample_mapping x
			  where x.trial_name = TrialId
			    and x.platform = 'SNP');
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from de_snp_copy_number',SQL%ROWCOUNT,stepCt,'Done');
		commit;
		
		delete from deapp.de_snp_subject_sorted_def s
		where s.trial_name = TrialId;
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from de_snp_subject_sorted_def',SQL%ROWCOUNT,stepCt,'Done');
		commit;

		select count(*) into pExists
		from all_tables
		where table_name = 'DE_SNP_DATA_BY_PROBE'
		  and partitioned = 'YES';
		  
		if pExists = 0 then
			--	table not partitioned, so just do delete
			delete from de_snp_data_by_probe
			where trial_name = TrialId;
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from de_snp_data_by_probe',SQL%ROWCOUNT,stepCt,'Done');
			commit;	
		else
			sqlText := 'alter table deapp.de_snp_data_by_probe drop PARTITION "' || TrialID || '"';
			execute immediate(sqlText);
			stepCt := stepCt + 1;
			cz_write_audit(jobId,databaseName,procedureName,'Dropped partition from de_snp_data_by_probe',0,stepCt,'Done');			
		end if;
		
		delete from de_subject_snp_dataset
		where trial_name = TrialId;
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from de_subject_snp_dataset',SQL%ROWCOUNT,stepCt,'Done');
		commit;		

	else
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'No SNP data to be deleted',0,stepCt,'Done');
	end if;		
	
	--	delete trial from de_subject_sample_mapping
	
	delete from de_subject_sample_mapping
	where trial_name = TrialId;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete trial from de_subject_sample_mapping',SQL%ROWCOUNT,stepCt,'Done');
	commit;
			
	--	delete data in biomart
	
	delete from bio_data_uid
	where unique_id = 'EXP:' || TrialId;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete trial from bio_data_uid',SQL%ROWCOUNT,stepCt,'Done');
	commit;
	
	delete from bio_data_compound
	where bio_data_id in 
		 (select x.bio_experiment_id from bio_experiment x
		  where x.accession = TrialId
		    and x.etl_id = 'METADATA:' || TrialId);
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete trial from bio_data_compound',SQL%ROWCOUNT,stepCt,'Done');
	commit;
	
	delete from bio_data_disease
	where bio_data_id in 
		 (select x.bio_experiment_id from bio_experiment x
		  where x.accession = TrialId
		    and x.etl_id = 'METADATA:' || TrialId);
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete trial from bio_data_disease',SQL%ROWCOUNT,stepCt,'Done');
	commit;
	
	delete from bio_data_taxonomy
	where bio_data_id in 
		 (select x.bio_experiment_id from bio_experiment x
		  where x.accession = TrialId
		    and x.etl_id = 'METADATA:' || TrialId);
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete trial from bio_data_taxonomy',SQL%ROWCOUNT,stepCt,'Done');
	commit;
	
	--	remove study from FMAPP
	
	delete from fmapp.fm_file ff
	where ff.file_id in
		 (select ffa.file_id
		  from fmapp.fm_folder f
			  ,fmapp.fm_folder_file_association ffa
		  where f.folder_name = TrialId
		    and f.folder_id = ffa.folder_id);
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete files from fm_file',SQL%ROWCOUNT,stepCt,'Done');
	
	delete from fmapp.fm_data_uid ff
	where ff.unique_id in
		 (select 'FIL:' || to_char(ffa.file_id)
		  from fmapp.fm_folder f
			  ,fmapp.fm_folder_file_association ffa
		  where f.folder_name = TrialId
		    and f.folder_id = ffa.folder_id);
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete files from fm_data_uid',SQL%ROWCOUNT,stepCt,'Done');
	
	delete from fmapp.fm_folder_association
	where object_uid = 'EXP:' || TrialId;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete trial from fm_folder_association',SQL%ROWCOUNT,stepCt,'Done');
	
	delete from fmapp.fm_data_uid ff
	where ff.unique_id in
		 (select 'FOL:' || to_char(f.folder_id)
		  from fmapp.fm_folder f
		  where f.folder_name = TrialId);
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete folders from fm_data_uid',SQL%ROWCOUNT,stepCt,'Done');
	
	delete from fmapp.fm_folder
	where folder_name = TrialId;
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Delete trial from fm_folder',SQL%ROWCOUNT,stepCt,'Done');
	commit;
	
	--	reload i2b2_secure
	
	i2b2_load_security_data;
	
  end if;
  
    ---Cleanup OVERALL JOB if this proc is being run standalone
  IF newJobFlag = 1
  THEN
    cz_end_audit (jobID, 'SUCCESS');
  END IF;

  EXCEPTION
  WHEN OTHERS THEN
    --Handle errors.
    cz_error_handler (jobID, procedureName);
    --End Proc
    cz_end_audit (jobID, 'FAIL');
  
END;