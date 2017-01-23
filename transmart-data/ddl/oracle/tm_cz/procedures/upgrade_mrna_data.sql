--
-- Type: PROCEDURE; Owner: TM_CZ; Name: UPGRADE_MRNA_DATA
--
  CREATE OR REPLACE PROCEDURE "TM_CZ"."UPGRADE_MRNA_DATA" 
(
 currentJobID NUMBER := null
)
AS
  
	--Audit variables
	newJobFlag 	INTEGER(1);
	databaseName 	VARCHAR(100);
	procedureName VARCHAR(100);
	jobID 		number(18,0);
	stepCt 		number(18,0);
  
	gexStudy	varchar2(200);
	gexSource	varchar2(200);
	pExists		int;
	tText		varchar2(2000);
  
	Type gex_study_rec is record
	(trial_name	varchar2(200)
	,source_cd	varchar2(200)
	);
  
	Type gex_study_tab is table of gex_study_rec;
  
	gex_study_array gex_study_tab;
  
	-- JEA@20120602	New

BEGIN

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
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Start upgrade_mrna_data',0,stepCt,'Done');
    COMMIT;
	
	--	get trial_names for all gex data
	
	select distinct trial_name
		  ,coalesce(source_cd,'STD') as source_cd
	bulk collect into gex_study_array
	from de_subject_sample_mapping
	where platform = 'MRNA_AFFYMETRIX'
	order by trial_name
			,source_cd;
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Bulk Collect trial_names',SQL%ROWCOUNT,stepCt,'Done');
	
	for i in gex_study_array.first .. gex_study_array.last
	loop
		gexStudy := gex_study_array(i).trial_name;
		gexSource := gex_study_array(i).source_cd;
		
		--	check if new table is partitioned and if partition exists
		
		select count(*) into pExists
		from all_tables
		where table_name = 'DE_SUBJECT_MICROARRAY_DATA_NEW'
		  and partitioned = 'YES';
		  
		if pExists > 0 then
			select count(*) into pExists
			from all_tab_partitions
			where table_name = 'DE_SUBJECT_MICROARRAY_DATA_NEW'
			  and partition_name = gexStudy || ':' || gexSource;
			  
			if pExists = 0 then
				tText := 'alter table deapp.de_subject_microarray_data_new add PARTITION "' || gexStudy || ':' || gexSource || 
						'"  VALUES (' || '''' || gexStudy || ':' || gexSource || '''' || ') ' ||
						   'NOLOGGING COMPRESS TABLESPACE "TRANSMART" ';
				execute immediate(tText);
				stepCt := stepCt + 1;
				cz_write_audit(jobId,databaseName,procedureName,'Added ' || gexStudy || ':' || gexSource || ' partition to de_subject_microarray_data_new',0,stepCt,'Done');
			end if;
		end if;
		
		insert into deapp.de_subject_microarray_data_new
		(trial_source
		,trial_name
		,probeset_id
		,assay_id
		,patient_id
		,raw_intensity
		,log_intensity
		,zscore
		)
		select sm.trial_name || ':' || coalesce(sm.source_cd,'STD')
			  ,sm.trial_name
			  ,sd.probeset_id
			  ,sm.assay_id
			  ,sm.patient_id
			  ,sd.raw_intensity
			  ,sd.log_intensity
			  ,sd.zscore
		from de_subject_sample_mapping sm
			,de_subject_microarray_data sd
		where sm.trial_name = gexStudy
		  and sm.source_cd = gexSource
		  and sm.platform = 'MRNA_AFFYMETRIX'
		  and sm.assay_id = sd.assay_id;
		  
		stepCt := stepCt + 1;
		cz_write_audit(jobId,databaseName,procedureName,'Inserted ' || gexStudy || ':' || gexSource || ' to new table',SQL%ROWCOUNT,stepCt,'Done');
		COMMIT;	
			
	end loop;
		
	--	drop indexes on de_subject_microarray_data
	
	i2b2_mrna_index_maint('DROP',null,jobId);
	
	--	rename existing de_subject_microarray_data to _old
	
	execute immediate('alter table deapp.de_subject_microarray_data rename to de_subject_microarray_data_old');
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Rename old de_subject_microarray_data',0,stepCt,'Done');
		
	--	rename _new to de_subject_microarray_data
	
	execute immediate('alter table deapp.de_subject_microarray_data_new rename to de_subject_microarray_data');
	
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'Rename old de_subject_microarray_data',0,stepCt,'Done');
		
	--	add indexes to de_subject_microarray_data
	
	i2b2_mrna_index_maint('ADD',null,jobId);
			
	stepCt := stepCt + 1;
	cz_write_audit(jobId,databaseName,procedureName,'End i2b2_audit',0,stepCt,'Done');
	
    COMMIT;
	--Cleanup OVERALL JOB if this proc is being run standalone
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
/
 
