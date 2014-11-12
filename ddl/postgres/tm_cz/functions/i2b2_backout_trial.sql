--
-- Name: i2b2_backout_trial(character varying, character varying, numeric); Type: FUNCTION; Schema: tm_cz; Owner: -
--
CREATE FUNCTION i2b2_backout_trial(trialid character varying, path_string character varying, currentjobid numeric) RETURNS integer
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$

/*************************************************************************
* Copyright 2008-2012 Janssen Research & Development, LLC.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
******************************************************************/
Declare

	rtnCd			integer;
	pExists			integer;
	sqlTxt			character varying;
	topNode			character varying;
	v_partition_id		text;

	--Audit variables
	newJobFlag		integer;
	databaseName		VARCHAR(100);
	procedureName		VARCHAR(100);
	jobID 			numeric(18,0);
	stepCt 			numeric(18,0);
	rowCt			numeric(18,0);
	errorNumber		character varying;
	errorMessage	        character varying;
	auditMessage	        character varying;

BEGIN

	stepCt := 0;
	
	--Set Audit Parameters
	newJobFlag := 0; -- False (Default)
	jobID := currentJobID;

	databaseName := 'TM_CZ';
	procedureName := 'I2b2_back_out_trial';
	
	--Audit JOB Initialization
	--If Job ID does not exist, then this is a single procedure run and we need to create it
	IF(jobID IS NULL or jobID < 1)
	THEN
		newJobFlag := 1; -- True
		select tm_cz.cz_start_audit (procedureName, databaseName) into jobId;
	END IF;

	-- trial id's are stored in upper case in the database
	trialid := upper(trialid);

	-- The second argument for this stored procedure (path_string) is deprecated.
	-- It's value is not independent of the value of the first argument (trialid) and
	-- therefore could be retrieved for the database. This prevents that errors are introduced
	-- in case inconsistent values are used for those arguments.
	-- For now, if a value is provided for path_string, it is checked against the information in the database
	-- and if there is no match, the procedure will be aborted.
	rowCt := 0;
	stepCt := stepCt + 1;
	if (path_string is not null AND length(path_string) > 0)
	then
		auditMessage := 'The use of the path_string argument for this stored procedure (i2b2_backout_trial) is deprecated';
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,auditMessage,rowCt,stepCt,'Done') into rtnCd;
	end if;	

	-- retrieve topNode for study with given trial id (trialid)
	begin
	select c_fullname from i2b2metadata.i2b2 where sourcesystem_cd = trialid and c_hlevel = 1 into topNode;
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

	-- check consistency between topNode and path_string
	rowCt := 0;
	stepCt := stepCt + 1;
	if (path_string is not null AND length(path_string) > 0 AND topNode is not null AND path_string != topNode)
	then
		errorNumber := '';
		errorMessage := 'Discrepancy between path_string argument value ('||path_string||') and value found in database ('||topNode||')';
		--Handle errors.
		select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
		--End Proc
		select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
		return -16;
	end if;

	-- delete all i2b2 nodes
	rowCt := 0;
	stepCt := stepCt + 1;
	if (topNode is null OR length(topNode) = 0)
	then
		auditMessage := 'Not able to retrieve top node associated with trial id: ' || trialid;
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,auditMessage,rowCt,stepCt,'Done') into rtnCd;

		auditMessage := 'Start deleting all data for trial ' || trialid;
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,auditMessage,rowCt,stepCt,'Done') into rtnCd;
	else
		auditMessage := 'Start deleting all data for trial ' || trialid || ' and topNode ' || topNode;
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,auditMessage,rowCt,stepCt,'Done') into rtnCd;

		select tm_cz.i2b2_delete_all_nodes(topNode,jobId) into rtnCd;	
	end if;

	--	delete clinical data
	
	begin
	delete from tm_lz.lz_src_clinical_data
	where study_id = trialid;
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from lz_src_clinical_data',rowCt,stepCt,'Done') into rtnCd;

	--	delete observation_fact SECURITY data, do before patient_dimension delete
	
	begin
	delete from i2b2demodata.observation_fact f
	where f.concept_cd = 'SECURITY'
	  and f.patient_num in
	     (select distinct p.patient_num from i2b2demodata.patient_dimension p
		  where p.sourcesystem_cd like trialid || ':%');
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete SECURITY data for trial from I2B2DEMODATA observation_fact',rowCt,stepCt,'Done') into rtnCd;

	--	delete patient data
	
	begin
	delete from i2b2demodata.patient_dimension
	where sourcesystem_cd like trialid || ':%';
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from I2B2DEMODATA patient_dimension',rowCt,stepCt,'Done') into rtnCd;

	begin
	delete from i2b2demodata.patient_trial
	where trial=  trialid;
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
	select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from I2B2DEMODATA patient_trial',rowCt,stepCt,'Done') into rtnCd;

--	delete gene expression data
	
	select count(*) into pExists
	from deapp.de_subject_sample_mapping
	where trial_name = TrialId
	  and platform = 'MRNA_AFFYMETRIX'
	  and trial_name = TrialId
	  and coalesce(omic_source_study,trial_name) = TrialId;

	if pExists > 0 then
		select distinct partition_id::text into v_partition_id
		from deapp.de_subject_sample_mapping
		where trial_name = TrialId
		  and platform = 'MRNA_AFFYMETRIX'
		  and coalesce(omic_source_study,trial_name) = TrialId;
		  
		sqlTxt := 'drop table if exists deapp.de_subject_microarray_data_' || v_partition_id;
		execute sqlTxt;
		stepCt := stepCt + 1;
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Drop partition table for de_subject_microarray_data',rowCt,stepCt,'Done') into rtnCd;
		
		begin
		delete from deapp.de_subject_sample_mapping
		where trial_name = TrialID
		  and platform = 'MRNA_AFFYMETRIX';
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
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from DEAPP de_subject_sample_mapping',rowCt,stepCt,'Done') into rtnCd;
		
	end if;
	
	--	delete acgh data
	
	select count(*) into pExists
	from deapp.de_subject_sample_mapping
	where trial_name = TrialId
	  and platform = 'ACGH'
	  and trial_name = TrialId
	  and coalesce(omic_source_study,trial_name) = TrialId;

	if pExists > 0 then
		select distinct partition_id::text into v_partition_id
		from deapp.de_subject_sample_mapping
		where trial_name = TrialId
		  and platform = 'ACGH'
		  and coalesce(omic_source_study,trial_name) = TrialId;
		  
		sqlTxt := 'drop table if exists deapp.de_subject_acgh_data_' || v_partition_id;
		execute sqlTxt;
		stepCt := stepCt + 1;
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Drop partition table for de_subject_acgh_data',rowCt,stepCt,'Done') into rtnCd;
		
		begin
		delete from deapp.de_subject_sample_mapping
		where trial_name = TrialID
		  and platform = 'ACGH';
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
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from DEAPP de_subject_sample_mapping',rowCt,stepCt,'Done') into rtnCd;
		
	end if;

	--	delete rnaseq data
	
	select count(*) into pExists
	from deapp.de_subject_sample_mapping
	where trial_name = TrialId
	  and platform = 'RNASEQ'
	  and trial_name = TrialId
	  and coalesce(omic_source_study,trial_name) = TrialId;

	if pExists > 0 then
		select distinct partition_id::text into v_partition_id
		from deapp.de_subject_sample_mapping
		where trial_name = TrialId
		  and platform = 'RNASEQ'
		  and coalesce(omic_source_study,trial_name) = TrialId;
		  
		sqlTxt := 'drop table if exists deapp.de_subject_rnaseq_data_' || v_partition_id;
		execute sqlTxt;
		stepCt := stepCt + 1;
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Drop partition table for de_subject_rnaseq_data',rowCt,stepCt,'Done') into rtnCd;
		
		begin
		delete from deapp.de_subject_sample_mapping
		where trial_name = TrialID
		  and platform = 'RNASEQ';
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
		select tm_cz.cz_write_audit(jobId,databaseName,procedureName,'Delete data for trial from DEAPP de_subject_sample_mapping',rowCt,stepCt,'Done') into rtnCd;
		
	end if;


      ---Cleanup OVERALL JOB if this proc is being run standalone
	IF newJobFlag = 1
	THEN
		select tm_cz.cz_end_audit (jobID, 'SUCCESS') into rtnCD;
	END IF;

	return 1;
	
EXCEPTION
WHEN OTHERS THEN
	errorNumber := SQLSTATE;
	errorMessage := SQLERRM;
	--Handle errors.
	select tm_cz.cz_error_handler (jobID, procedureName, errorNumber, errorMessage) into rtnCd;
	--End Proc
	select tm_cz.cz_end_audit (jobID, 'FAIL') into rtnCd;
	return -16;
END;

$$;

